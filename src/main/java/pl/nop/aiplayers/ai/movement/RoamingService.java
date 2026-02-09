package pl.nop.aiplayers.ai.movement;

import org.bukkit.Location;
import pl.nop.aiplayers.ai.Action;
import pl.nop.aiplayers.model.AIPlayerProfile;
import pl.nop.aiplayers.model.AIPlayerSession;

import java.util.Random;
import java.util.UUID;

public class RoamingService {
    private static final String STATE_KEY = "roamState";
    private static final double STEP_VARIANCE_MIN = 0.85;
    private static final double STEP_VARIANCE_MAX = 1.15;

    private final RoamingConfig config;

    public RoamingService(RoamingConfig config) {
        this.config = config;
    }

    public Action buildRoamAction(AIPlayerSession session, Location current) {
        if (current == null) {
            return Action.idle();
        }
        RoamState state = getOrCreateState(session, current);
        long now = System.currentTimeMillis();
        state.ensureSpawn(session.getProfile(), current);
        state.ensureRadius(session.getProfile());
        if (state.state == RoamMode.IDLE) {
            return handleIdle(state, now);
        }
        return handleMoving(state, current, now);
    }

    private Action handleIdle(RoamState state, long now) {
        if (now < state.nextDecisionAtMs) {
            return Action.idle();
        }
        if (state.rng.nextDouble() < config.getIdleChance()) {
            state.nextDecisionAtMs = now + randomBetween(state.rng, config.getIdleMinMs(), config.getIdleMaxMs());
            return Action.idle();
        }
        state.currentTarget = randomTarget(state);
        state.state = RoamMode.MOVING;
        state.nextDecisionAtMs = now;
        return Action.idle();
    }

    private Action handleMoving(RoamState state, Location current, long now) {
        if (state.currentTarget == null || state.currentTarget.getWorld() == null || !isInsideRadius(state, state.currentTarget)) {
            state.currentTarget = randomTarget(state);
        }
        double distance = state.currentTarget.distance(current);
        double reachThreshold = Math.max(config.getReachThreshold(), state.baseStepSize);
        if (distance <= reachThreshold) {
            state.currentTarget = null;
            state.state = RoamMode.IDLE;
            state.nextDecisionAtMs = now + randomBetween(state.rng, config.getMinDelayMs(), config.getMaxDelayMs());
            return Action.idle();
        }
        Location step = buildStep(state, current, state.currentTarget);
        if (step == null) {
            state.currentTarget = null;
            state.state = RoamMode.IDLE;
            state.nextDecisionAtMs = now + randomBetween(state.rng, config.getMinDelayMs(), config.getMaxDelayMs());
            return Action.idle();
        }
        return Action.moveTo(step);
    }

    private Location buildStep(RoamState state, Location current, Location target) {
        double dx = target.getX() - current.getX();
        double dz = target.getZ() - current.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance == 0) {
            return null;
        }
        double variance = STEP_VARIANCE_MIN + (STEP_VARIANCE_MAX - STEP_VARIANCE_MIN) * state.rng.nextDouble();
        double stepSize = clamp(state.baseStepSize * variance, config.getStepMin(), config.getStepMax());
        double scale = stepSize / distance;
        Location step = current.clone();
        step.add(dx * scale, 0, dz * scale);
        step.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        step.setPitch(0f);
        double currentDistanceSq = current.distanceSquared(state.spawnLocation);
        if (!isInsideRadius(state, step)) {
            if (currentDistanceSq > state.roamRadius * state.roamRadius) {
                double stepDistanceSq = step.distanceSquared(state.spawnLocation);
                if (stepDistanceSq <= currentDistanceSq) {
                    return step;
                }
            }
            state.currentTarget = randomTarget(state);
            dx = state.currentTarget.getX() - current.getX();
            dz = state.currentTarget.getZ() - current.getZ();
            distance = Math.sqrt(dx * dx + dz * dz);
            if (distance == 0) {
                return null;
            }
            scale = stepSize / distance;
            step = current.clone();
            step.add(dx * scale, 0, dz * scale);
            step.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
            step.setPitch(0f);
            if (!isInsideRadius(state, step)) {
                if (currentDistanceSq > state.roamRadius * state.roamRadius) {
                    double stepDistanceSq = step.distanceSquared(state.spawnLocation);
                    if (stepDistanceSq <= currentDistanceSq) {
                        return step;
                    }
                }
                return null;
            }
        }
        return step;
    }

    private Location randomTarget(RoamState state) {
        double angle = state.rng.nextDouble() * Math.PI * 2;
        double distance = Math.sqrt(state.rng.nextDouble()) * state.roamRadius;
        double x = state.spawnLocation.getX() + Math.cos(angle) * distance;
        double z = state.spawnLocation.getZ() + Math.sin(angle) * distance;
        double y = state.spawnLocation.getY();
        return new Location(state.spawnLocation.getWorld(), x, y, z);
    }

    private boolean isInsideRadius(RoamState state, Location location) {
        return location != null && location.getWorld() != null
                && location.distanceSquared(state.spawnLocation) <= state.roamRadius * state.roamRadius;
    }

    private RoamState getOrCreateState(AIPlayerSession session, Location current) {
        Object stateObj = session.getRuntimeMemory().get(STATE_KEY);
        if (stateObj instanceof RoamState) {
            return (RoamState) stateObj;
        }
        RoamState state = new RoamState(session.getProfile(), current, config);
        session.getRuntimeMemory().put(STATE_KEY, state);
        return state;
    }

    private long randomBetween(Random rng, long min, long max) {
        if (max <= min) {
            return min;
        }
        return min + (long) (rng.nextDouble() * (max - min));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum RoamMode {
        IDLE,
        MOVING
    }

    private static class RoamState {
        private final Random rng;
        private Location spawnLocation;
        private double roamRadius;
        private Location currentTarget;
        private long nextDecisionAtMs;
        private double baseStepSize;
        private RoamMode state;

        private RoamState(AIPlayerProfile profile, Location current, RoamingConfig config) {
            this.spawnLocation = profile.getSpawnLocation() != null ? profile.getSpawnLocation().clone() : current.clone();
            this.roamRadius = Math.max(profile.getRoamRadius(), 1.0);
            this.rng = createRandom(profile.getUuid(), current);
            this.baseStepSize = randomBetween(rng, config.getStepMin(), config.getStepMax());
            this.state = RoamMode.IDLE;
            this.nextDecisionAtMs = System.currentTimeMillis() + randomBetween(rng, config.getMinDelayMs(), config.getMaxDelayMs());
        }

        private void ensureSpawn(AIPlayerProfile profile, Location current) {
            Location profileSpawn = profile.getSpawnLocation();
            if (profileSpawn == null) {
                if (spawnLocation == null) {
                    spawnLocation = current.clone();
                }
                return;
            }
            if (spawnLocation == null || !spawnLocation.getWorld().equals(profileSpawn.getWorld())
                    || spawnLocation.distanceSquared(profileSpawn) > 0.25) {
                spawnLocation = profileSpawn.clone();
            }
        }

        private void ensureRadius(AIPlayerProfile profile) {
            roamRadius = Math.max(profile.getRoamRadius(), 1.0);
        }

        private static Random createRandom(UUID uuid, Location current) {
            long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
            if (current != null && current.getWorld() != null) {
                seed ^= current.getWorld().getSeed();
            }
            return new Random(seed);
        }

        private static double randomBetween(Random rng, double min, double max) {
            if (max <= min) {
                return min;
            }
            return min + (rng.nextDouble() * (max - min));
        }

        private static long randomBetween(Random rng, long min, long max) {
            if (max <= min) {
                return min;
            }
            return min + (long) (rng.nextDouble() * (max - min));
        }
    }
}
