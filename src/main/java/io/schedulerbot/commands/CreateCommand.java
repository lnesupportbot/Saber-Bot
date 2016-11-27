package io.schedulerbot.commands;

import io.schedulerbot.Main;
import io.schedulerbot.utils.BotConfig;
import io.schedulerbot.utils.EventEntryParser;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * file: CreateCommand.java
 *
 * CreateCommand places a new event on the EVENT_CHAN text channel
 * Note that the actual EventEntry thread has not yet been created.
 */
public class CreateCommand implements Command
{
    private static final String USAGE_EXTENDED = "Event entries can be initialized using the form **!create " +
            "\"TITLE\" <Start> <End> <Optional Arguments>**. Entries MUST be initialized with a title, a start " +
            "time, and an end time. Start and end times should be of form HH:mm. Entries can optionally be " +
            "configured with comments, repeat, and a start date. Adding **repeat no**/**daily**/**weekly** to " +
            "**<Optional>** will configure repeat; default behavior is no repeat. Adding **date MM/dd** to " +
            "**<Optional>** will configure the start date; default behavior is to use the current date or the " +
            "next day depending on if the current time is greater than the start time. Comments may be added by" +
            " adding **\"YOUR COMMENT\"** in **<Optional>**; any number of comments may be added in **<Optional>" +
            "**.\nEx. **!create \"Weekly Raid Event\" 19:00 22:00 repeat weekly \"Healers and tanks always in " +
            "demand.\" \"PM our raid captain with your role and level if attending\"**";

    private static final String USAGE_BRIEF = "**" + BotConfig.PREFIX + "create** - Generates a new event entry" +
            " in #" + BotConfig.EVENT_CHAN + ".";

    @Override
    public String help(boolean brief)
    {
        if( brief )
            return USAGE_BRIEF;
        else
            return USAGE_BRIEF + "\n" + USAGE_EXTENDED;
    }
    @Override
    public boolean verify(String[] args, MessageReceivedEvent event)
    {
        // TODO do some actually verification
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event)
    {
        if( Main.entriesByGuild.containsKey( event.getGuild().getId() )
                && Main.entriesByGuild.get( event.getGuild().getId() ).size() >= BotConfig.MAX_ENTRIES
                && BotConfig.MAX_ENTRIES > 0)
        {
            String msg = "Your guild already has the maximum allowed amount of event entries."
                    +" No more entries may be added until old entries are destroyed.";
            Main.sendMsg( msg, event.getChannel() );
            return;
        }

        String eTitle = "";
        String eStart = "00:00";    // initialized just in case verify failed it's duty
        String eEnd = "00:00";      //
        ArrayList<String> eComments = new ArrayList<String>();
        int eRepeat = 0;             // default is 0 (no repeat)
        LocalDate eDate = LocalDate.now();

        String buffComment = "";    // String to generate comments strings to place in eComments

        boolean flag1 = false;  // true if 'eTitle' has been grabbed from args
        boolean flag2 = false;  // true if 'eStart' has been grabbed from args
        boolean flag3 = false;  // true if 'eEnd' has been grabbed from args
        boolean flag4 = false;  // true if a comment argument was found and is being processed,
                                // false when the last arg forming the comment is found
        boolean flag5 = false;  // true if an arg=='repeat' when flag4 is not flagged
                                // when true, reads the next arg
        boolean flag6 = false;

        for( String arg : args )
        {
            if(!flag1)
            {
                if( arg.endsWith("\"") )
                {
                    flag1 = true;
                    eTitle += arg.replace("\"", "");
                }
                else
                    eTitle += arg.replace("\"", "") + " ";
            }
            else if(!flag2)
            {
                flag2 = true;
                eStart = arg;
            }
            else if(!flag3)
            {
                flag3 = true;
                eEnd = arg;
            }
            else
            {
                if( flag5 )
                {
                    if( arg.equals("daily") )
                        eRepeat = 1;
                    else if( arg.equals("weekly") )
                        eRepeat = 2;
                    else if( Character.isDigit(arg.charAt(0)) && Integer.parseInt(arg)==1 )
                        eRepeat = 1;
                    else if ( Character.isDigit(arg.charAt(0)) && Integer.parseInt(arg)==2)
                        eRepeat = 2;
                    flag5 = false;
                }
                if( !flag4 && !flag6 && arg.equals("repeat") )
                {
                    flag5 = true;
                }

                if( flag6 )
                {
                    if( arg.toLowerCase().equals("today") )
                        eDate = LocalDate.now();
                    else if( arg.toLowerCase().equals("tomorrow") )
                        eDate = LocalDate.now().plusDays( 1 );
                    else if( Character.isDigit(arg.charAt(0)) )
                    {
                        eDate = eDate.withMonth(Integer.parseInt(arg.split("/")[0]));
                        eDate = eDate.withDayOfMonth(Integer.parseInt(arg.split("/")[1]));
                    }
                    flag6 = false;
                }
                else if( !flag4 && !flag5 && arg.equals("date"))
                {
                    flag6 = true;
                }

                if( arg.startsWith("\"") )
                    flag4 = true;
                if( flag4 )
                    buffComment += arg.replace("\"","");
                if( arg.endsWith("\"") )
                {
                    flag4 = false;
                    eComments.add(buffComment);
                    buffComment = "";
                }
                else if( flag4 )
                    buffComment += " ";
            }
        }

        if( (Integer.parseInt(eStart.split(":")[0])*60+Integer.parseInt(eStart.split(":")[1])
                < LocalTime.now().getHour()*60+LocalTime.now().getMinute()) && LocalDate.now().equals(eDate) )
            eDate = eDate.plusDays(1);

        // generate the event entry message
        String msg = EventEntryParser.generate( eTitle, eStart, eEnd, eComments, eRepeat, eDate, null );

        Main.sendMsg( msg, event.getGuild().getTextChannelsByName(BotConfig.EVENT_CHAN, false).get(0) );
    }
}