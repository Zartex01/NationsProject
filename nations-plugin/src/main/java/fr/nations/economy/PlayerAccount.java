package fr.nations.economy;

import java.util.UUID;

public class PlayerAccount {

    private final UUID playerId;
    private String playerName;
    private double balance;

    public PlayerAccount(UUID playerId, double balance) {
        this.playerId = playerId;
        this.balance = balance;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public double getBalance() { return balance; }

    public void setBalance(double balance) {
        this.balance = Math.max(0, balance);
    }

    public void deposit(double amount) {
        if (amount > 0) this.balance += amount;
    }

    public boolean withdraw(double amount) {
        if (amount <= 0 || balance < amount) return false;
        this.balance -= amount;
        return true;
    }

    public boolean has(double amount) {
        return balance >= amount;
    }
}
