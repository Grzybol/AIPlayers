package pl.nop.aiplayers.ai;

import org.bukkit.Location;

public class Action {
    private final ActionType type;
    private final Location targetLocation;
    private final String message;
    private final String targetPlayer;
    private final String itemId;
    private final int amount;
    private final double price;

    private Action(ActionType type, Location targetLocation, String message, String targetPlayer, String itemId, int amount, double price) {
        this.type = type;
        this.targetLocation = targetLocation;
        this.message = message;
        this.targetPlayer = targetPlayer;
        this.itemId = itemId;
        this.amount = amount;
        this.price = price;
    }

    public static Action moveTo(Location location) {
        return new Action(ActionType.MOVE_TO, location, null, null, null, 0, 0);
    }

    public static Action say(String message) {
        return new Action(ActionType.SAY, null, message, null, null, 0, 0);
    }

    public static Action idle() {
        return new Action(ActionType.IDLE, null, null, null, null, 0, 0);
    }

    public static Action follow(String playerName) {
        return new Action(ActionType.FOLLOW_PLAYER, null, null, playerName, null, 0, 0);
    }

    public static Action lookAt(Location location) {
        return new Action(ActionType.LOOK_AT, location, null, null, null, 0, 0);
    }

    public static Action buy(String itemId, int amount, double price) {
        return new Action(ActionType.BUY_ITEM, null, null, null, itemId, amount, price);
    }

    public static Action sell(String itemId, int amount, double price) {
        return new Action(ActionType.SELL_ITEM, null, null, null, itemId, amount, price);
    }

    public ActionType getType() {
        return type;
    }

    public Location getTargetLocation() {
        return targetLocation;
    }

    public String getMessage() {
        return message;
    }

    public String getTargetPlayer() {
        return targetPlayer;
    }

    public String getItemId() {
        return itemId;
    }

    public int getAmount() {
        return amount;
    }

    public double getPrice() {
        return price;
    }
}
