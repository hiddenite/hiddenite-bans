package eu.hiddenite.bans.helpers;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private DurationParser() {}

    private static final int[] durationFields =
            { Calendar.DAY_OF_YEAR, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND };
    private static final String[] durationLabels =
            { "d", "h", "m", "s" };

    public static Timestamp parse(String input) {
        Calendar output = Calendar.getInstance();
        boolean found = false;
        for (int i = 0; i < durationFields.length; i++) {
            Pattern p = Pattern.compile("(\\d+)" + durationLabels[i] + ".*");
            Matcher m = p.matcher(input);
            if (m.matches())
            {
                found = true;
                output.add(durationFields[i], Integer.parseInt(m.group(1)));
                input = input.replace(m.group(1) + durationLabels[i], "");
                if (input.length() == 0)
                    break;
            }
        }
        if (found) {
            return new Timestamp(output.getTimeInMillis());
        }
        return null;
    }
}
