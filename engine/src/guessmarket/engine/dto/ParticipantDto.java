package guessmarket.engine.dto;

import java.util.List;

public class ParticipantDto
{
    private final String userName;
    private final List<Integer> shares;
    private final List<Double> netPaid;
    private final double commissionPaid;

    public ParticipantDto(String userName, List<Integer> shares, List<Double> netPaid, double commissionPaid)
    {
        this.userName = userName;
        this.shares = shares;
        this.netPaid = netPaid;
        this.commissionPaid = commissionPaid;
    }

    public String getUserName()
    {
        return userName;
    }

    public List<Integer> getShares()
    {
        return shares;
    }

    public List<Double> getNetPaid()
    {
        return netPaid;
    }

    public double getCommissionPaid()
    {
        return commissionPaid;
    }
}
