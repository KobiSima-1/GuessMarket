package guessmarket.engine.model;

import java.io.Serializable;

public class Account implements Serializable
{
    private static final long serialVersionUID = 1L;

    private double balance;

    public Account(double initialBalance)
    {
        balance = initialBalance;
    }

    public double getBalance()
    {
        return balance;
    }

    public boolean canAfford(double amount)
    {
        return balance >= amount;
    }

    public void deposit(double amount)
    {
        balance += amount;
    }

    public void withdraw(double amount)
    {
        balance -= amount;
    }
}
