package fr.bde_eseo.eseomega.lacommande.model;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Rascafr on 20/07/2015.
 */
public class HistoryItem {

    public final static int STATUS_PREPARING = 0;
    public final static int STATUS_READY = 1;
    public final static int STATUS_DONE = 2;
    public final static int STATUS_NOPAID = 3;

    private String commandName;
    private int commandStatus;
    private String commandDate, commandStr;
    private double commandPrice;
    private boolean isHeader, isFooter;
    private int commandNumber, commandModulo;

    public HistoryItem (String commandName, int commandStatus, double commandPrice, String commandDate, int commandNumber) {
        this.commandName = commandName;
        this.commandStatus = commandStatus;
        this.commandPrice = commandPrice;
        this.commandDate = commandDate;
        this.commandDate = getFrenchDate();
        this.commandNumber = commandNumber;
        this.isHeader = false;
    }

    public HistoryItem (String commandName, int commandStatus, double commandPrice, String commandDate, int commandNumber, int commandModulo, String commandStr) {
        this.commandName = commandName;
        this.commandStatus = commandStatus;
        this.commandPrice = commandPrice;
        this.commandDate = commandDate;
        this.commandDate = getFrenchDate();
        this.commandNumber = commandNumber;
        this.isHeader = false;
        this.commandModulo = commandModulo;
        this.commandStr = commandStr;
    }

    /*
    @Deprecated
    public HistoryItem (String commandName, int commandStatus, double commandPrice, String commandDate) {
        this.commandName = commandName;
        this.commandStatus = commandStatus;
        this.commandPrice = commandPrice;
        this.commandDate = commandDate;
        this.isHeader = false;
    }*/

    // Command for today
    public HistoryItem (String commandName, int commandStatus, double commandPrice) {
        this.commandName = commandName;
        this.commandStatus = commandStatus;
        this.commandPrice = commandPrice;
        this.isHeader = false;
        this.isFooter = false;

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE);

        this.commandDate = sdf.format(Calendar.getInstance(TimeZone.getDefault(), Locale.FRANCE).getTime());
    }

    // Header / footer (if true)
    public HistoryItem (String titleName, boolean isFooter) {
        this.commandName = titleName;
        this.isHeader = true;
        this.isFooter = isFooter;
    }

    public String getCommandName() {
        return commandName;
    }

    public int getCommandStatus() {
        return commandStatus;
    }

    public String getCommandStatusAsString () {
        switch (commandStatus) {
            case STATUS_DONE:
                return "Commande terminée";
            case STATUS_PREPARING:
                return "En préparation";
            case STATUS_READY:
                return "Commande prête";
            case STATUS_NOPAID:
                return "Commande non payée";
            default:
                return "Erreur dans la commande";
        }
    }

    public String getCommandDate() {
        return commandDate;
    }

    public double getCommandPrice() {
        return commandPrice;
    }

    public String getCommandPriceAsString(){
        return new DecimalFormat("0.00").format(commandPrice) + "€";
    }

    public String getCommandNumberAsString() {
        return commandStr + new DecimalFormat("000").format(commandModulo);
    }

    public boolean isHeader() {
        return isHeader;
    }

    public boolean isFooter() {
        return isFooter;
    }

    public int getCommandNumber() {
        return commandNumber;
    }

    /*public String getCommandNumberAsString() {
        return "№" + commandNumber;
    }*/

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public void setCommandStatus(int commandStatus) {
        this.commandStatus = commandStatus;
    }

    public void setCommandDate(String commandDate) {
        this.commandDate = commandDate;
    }

    public void setCommandPrice(double commandPrice) {
        this.commandPrice = commandPrice;
    }

    public String toString() {
        return "Command Data = {\""+getCommandName()+"\" the "+getCommandDate()+", price = "+commandPrice+"€, status = "+getCommandStatusAsString()+"}";
    }

    public Date getParsedDate () {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE);
        Date date = null;
        try {
            date = format.parse(this.commandDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public String getFrenchDate () {
        Date d = getParsedDate();
        SimpleDateFormat sdf = new SimpleDateFormat("E dd MMMM yyyy, 'à' HH:mm", Locale.FRANCE);
        return sdf.format(d);
    }
}