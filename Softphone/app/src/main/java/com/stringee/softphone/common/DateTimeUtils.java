package com.stringee.softphone.common;

import android.content.Context;
import android.util.Log;

import com.stringee.softphone.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeUtils {

    /**
     * @param date1
     * @param date2
     * @return
     * @author nguyenbaluan
     * @des check two date in same week
     */
    public static boolean checkDateInWeek(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setFirstDayOfWeek(Calendar.MONDAY);
        Calendar cal2 = Calendar.getInstance();
        cal2.setFirstDayOfWeek(Calendar.MONDAY);
        cal1.setTime(date1);
        cal2.setTime(date2);
        if ((cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR))
                && (cal1.get(Calendar.DAY_OF_WEEK) > cal2.get(Calendar.DAY_OF_WEEK))) {
            return true;
        }

        return false;
    }

    /**
     * @param date
     * @param context
     * @return
     * @author nguyenbaluan
     * @des get day in week of a date in abbreviation
     */
    public static String getDay(Date date, Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.MONDAY:
                return context.getString(R.string.monday);
            case Calendar.TUESDAY:
                return context.getString(R.string.tuesday);
            case Calendar.WEDNESDAY:
                return context.getString(R.string.wednesday);
            case Calendar.THURSDAY:
                return context.getString(R.string.thursday);
            case Calendar.FRIDAY:
                return context.getString(R.string.friday);
            case Calendar.SATURDAY:
                return context.getString(R.string.saturday);
            case Calendar.SUNDAY:
                return context.getString(R.string.sunday);
        }
        return "";
    }

    // /**
    // * @author nguyenbaluan
    // * @des get day, month of a date
    // * @param date
    // * @param context
    // * @return
    // */
    // public static String getDate(Date date, Context context) {
    // Calendar cal = Calendar.getInstance();
    // cal.setTime(date);
    // int day = cal.get(Calendar.DAY_OF_MONTH);
    // int month = cal.get(Calendar.MONTH);
    // String strMonth = "";
    // switch (month) {
    // case Calendar.JANUARY:
    // strMonth = context.getString(R.string.jan);
    // break;
    // case Calendar.FEBRUARY:
    // strMonth = context.getString(R.string.feb);
    // break;
    // case Calendar.MARCH:
    // strMonth = context.getString(R.string.mar);
    // break;
    // case Calendar.APRIL:
    // strMonth = context.getString(R.string.apr);
    // break;
    // case Calendar.MAY:
    // strMonth = context.getString(R.string.may);
    // break;
    // case Calendar.JUNE:
    // strMonth = context.getString(R.string.jun);
    // break;
    // case Calendar.JULY:
    // strMonth = context.getString(R.string.jul);
    // break;
    // case Calendar.AUGUST:
    // strMonth = context.getString(R.string.aug);
    // break;
    // case Calendar.SEPTEMBER:
    // strMonth = context.getString(R.string.sep);
    // break;
    // case Calendar.OCTOBER:
    // strMonth = context.getString(R.string.oct);
    // break;
    // case Calendar.NOVEMBER:
    // strMonth = context.getString(R.string.nov);
    // break;
    // case Calendar.DECEMBER:
    // strMonth = context.getString(R.string.dec);
    // break;
    //
    // default:
    // break;
    // }
    // int year = cal.get(Calendar.YEAR);
    // Calendar cal2 = Calendar.getInstance();
    // cal2.setTime(new Date());
    // int currentYear = cal2.get(Calendar.YEAR);
    // if (year != currentYear) {
    // return context.getString(R.string.day) + " " + day + " " + strMonth
    // + " " + year;
    // } else {
    // return context.getString(R.string.day) + " " + day + " " + strMonth;
    // }
    // }

    // /**
    // * @author nguyenbaluan
    // * @des return time in am pm format
    // * @param strDate
    // * @return
    // */
    // public static String getTime(long time) {
    // Date date = new Date(time);
    // Calendar cal = Calendar.getInstance();
    // cal.setTime(date);
    // int hour = cal.get(Calendar.HOUR);
    // int min = cal.get(Calendar.MINUTE);
    // String strMin = "";
    // if (min < 10) {
    // strMin = "0" + min;
    // } else {
    // strMin = String.valueOf(min);
    // }
    // int ampm = cal.get(Calendar.AM_PM);
    // if (hour == 0) {
    // hour = 12;
    // }
    // if (ampm == Calendar.AM) {
    // return hour + ":" + strMin + " AM";
    // } else {
    // return hour + ":" + strMin + " PM";
    // }
    // }

    /**
     * @param date
     * @return
     * @author nguyenbaluan
     * @des return time in am pm format
     */
    public static String getTime(Date date) {
        if (date != null) {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm");
            return format.format(date);
        }
        return "";
    }

    public static long getMilisecondByString(String datetime) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(Constant.DATETIME_FORMAT);
            Date date = format.parse(datetime);
            return date.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * get dateTime cho message va conversation
     *
     * @param date
     * @return
     */
    public static String getStringTime(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(Constant.DATETIME_FORMAT);
        return format.format(date);
    }

    public static boolean isSameDay(Date date1, Date date2) {
        SimpleDateFormat format = new SimpleDateFormat(Constant.DATETIME_FORMAT);
        String datetime1 = format.format(date1);
        String datetime2 = format.format(date2);
        if (datetime1.substring(0, 8).equals(datetime2.substring(0, 8))) {
            return true;
        }

        return false;
    }

    //
    // public static boolean isSameDay(long time1, long time2) {
    // final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
    // // Strip out the time part of each date.
    // long julianDayNumber1 = time1 / MILLIS_PER_DAY;
    // long julianDayNumber2 = time2 / MILLIS_PER_DAY;
    //
    // // If they now are equal then it is the same day.
    // return julianDayNumber1 == julianDayNumber2;
    // }

    public static boolean isSameDay(String datetime1, String datetime2) {
        if (datetime1 == null || datetime1.length() < 8 || datetime2 == null || datetime2.length() < 8) {
            return false;
        }
        return datetime1.substring(0, 8).equals(datetime2.substring(0, 8));
    }

    public static String getDateByLongMilisecond(long time) {
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        return format.format(date);
    }

    public static String getHourMinuteTime(long time) {
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        return format.format(date);
    }

    public static String formatDate(long time, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(new Date(time));
    }

    /**
     * @param date1
     * @param date2
     * @return
     * @author nguyenbaluan
     * @des check two date in same year
     */
    public static boolean isSameYear(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        if (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)) {
            return true;
        }

        return false;
    }

    public static boolean isYesterday(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        if (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) - cal2.get(Calendar.DAY_OF_YEAR) == 1) {
            return true;
        }

        return false;
    }

    /**
     * Get free call time from duration
     *
     * @param duration
     * @return
     */
    public static String getCallTime(long duration) {
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String res = format.format(new Date(duration));
        return res == null ? "00:00" : res;
    }

    /**
     * Get free call time
     *
     * @param currentTime
     * @param startTime
     * @return
     */
    public static String getCallTime(long currentTime, long startTime) {
        long time = currentTime - startTime;
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(new Date(time));
    }

    /**
     * @param datetime
     * @param context
     * @return datetime for conversation to display in list conversations
     */
    public static String getHeaderTime(String datetime, Context context) {
        String result = "";
        try {
            Date currentDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat(Constant.DATETIME_FORMAT);
            Date lastDate = format.parse(datetime);
            if (DateTimeUtils.isSameDay(currentDate, lastDate)) {
                result = context.getString(R.string.today);
            } else if (DateTimeUtils.isYesterday(currentDate, lastDate)) {
                result = context.getString(R.string.yesterday);
            } else if (DateTimeUtils.checkDateInWeek(currentDate, lastDate)) {
                result = DateTimeUtils.getDay(lastDate, context);
            } else if (DateTimeUtils.isSameYear(currentDate, lastDate)) {
                result = DateTimeUtils.formatDate(lastDate.getTime(), "dd/MM");
            } else {
                result = DateTimeUtils.formatDate(lastDate.getTime(), "dd/MM/yyyy");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
