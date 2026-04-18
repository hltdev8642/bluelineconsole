package net.nhiroki.bluelineconsole.commandSearchers.eachSearcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.AlarmClock;
import android.view.View;

import androidx.annotation.NonNull;

import net.nhiroki.bluelineconsole.applicationMain.MainActivity;
import net.nhiroki.bluelineconsole.interfaces.CandidateEntry;
import net.nhiroki.bluelineconsole.interfaces.CommandSearcher;
import net.nhiroki.bluelineconsole.interfaces.EventLauncher;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimerAlarmCommandSearcher implements CommandSearcher {

    private static final Pattern TIMER_MMSS = Pattern.compile(
        "timer\\s+(\\d+):(\\d{2})\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIMER_NUM_ONLY = Pattern.compile(
        "timer\\s+(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALARM_PATTERN = Pattern.compile(
        "alarm\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*$", Pattern.CASE_INSENSITIVE);

    @Override public void refresh(Context context) {}
    @Override public void close() {}
    @Override public boolean isPrepared() { return true; }
    @Override public void waitUntilPrepared() {}

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();
        String q = query.trim();

        TimerSpec timer = parseTimer(q);
        if (timer != null) {
            candidates.add(new TimerAlarmEntry("Set timer: " + timer.displayText, timer.seconds, -1, -1));
        }

        AlarmSpec alarm = parseAlarm(q);
        if (alarm != null) {
            candidates.add(new TimerAlarmEntry("Set alarm: " + alarm.displayText, -1, alarm.hour, alarm.minute));
        }

        return candidates;
    }

    private TimerSpec parseTimer(String q) {
        // Format: timer 1:30 (mm:ss)
        Matcher m = TIMER_MMSS.matcher(q);
        if (m.matches()) {
            int minutes = Integer.parseInt(m.group(1));
            int seconds = Integer.parseInt(m.group(2));
            int total = minutes * 60 + seconds;
            return new TimerSpec(total, minutes + "m " + seconds + "s");
        }

        String timerPrefix = "timer ";
        if (!q.toLowerCase().startsWith(timerPrefix)) return null;
        String rest = q.substring(timerPrefix.length()).trim();
        if (rest.isEmpty()) return null;

        int totalSeconds = 0;
        StringBuilder displayBuilder = new StringBuilder();

        Pattern hPat = Pattern.compile("(\\d+)\\s*h(?:ours?)?", Pattern.CASE_INSENSITIVE);
        Pattern mPat = Pattern.compile("(\\d+)\\s*m(?:in(?:utes?)?)?(?!s)", Pattern.CASE_INSENSITIVE);
        Pattern sPat = Pattern.compile("(\\d+)\\s*s(?:ec(?:onds?)?)?", Pattern.CASE_INSENSITIVE);

        Matcher hm = hPat.matcher(rest);
        if (hm.find()) {
            int h = Integer.parseInt(hm.group(1));
            totalSeconds += h * 3600;
            displayBuilder.append(h).append("h ");
        }
        Matcher mm = mPat.matcher(rest);
        if (mm.find()) {
            int min = Integer.parseInt(mm.group(1));
            totalSeconds += min * 60;
            displayBuilder.append(min).append("m ");
        }
        Matcher sm = sPat.matcher(rest);
        if (sm.find()) {
            int sec = Integer.parseInt(sm.group(1));
            totalSeconds += sec;
            displayBuilder.append(sec).append("s");
        }

        if (totalSeconds == 0) {
            Matcher numOnly = TIMER_NUM_ONLY.matcher(q);
            if (numOnly.matches()) {
                int min = Integer.parseInt(numOnly.group(1));
                totalSeconds = min * 60;
                displayBuilder.append(min).append(" min");
            } else {
                return null;
            }
        }

        return new TimerSpec(totalSeconds, displayBuilder.toString().trim());
    }

    private AlarmSpec parseAlarm(String q) {
        Matcher m = ALARM_PATTERN.matcher(q);
        if (!m.matches()) return null;
        int hour = Integer.parseInt(m.group(1));
        int minute = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
        String ampm = m.group(3);
        if (ampm != null) {
            if (ampm.equalsIgnoreCase("pm") && hour != 12) hour += 12;
            if (ampm.equalsIgnoreCase("am") && hour == 12) hour = 0;
        }
        if (hour > 23 || minute > 59) return null;
        String display = String.format("%02d:%02d", hour, minute);
        return new AlarmSpec(hour, minute, display);
    }

    private static class TimerSpec {
        final int seconds;
        final String displayText;
        TimerSpec(int s, String d) { this.seconds = s; this.displayText = d; }
    }

    private static class AlarmSpec {
        final int hour, minute;
        final String displayText;
        AlarmSpec(int h, int m, String d) { this.hour = h; this.minute = m; this.displayText = d; }
    }

    private static class TimerAlarmEntry implements CandidateEntry {
        private final String title;
        private final int timerSeconds;
        private final int alarmHour, alarmMinute;

        TimerAlarmEntry(String title, int timerSeconds, int alarmHour, int alarmMinute) {
            this.title = title;
            this.timerSeconds = timerSeconds;
            this.alarmHour = alarmHour;
            this.alarmMinute = alarmMinute;
        }

        @Override public String getTitle() { return title; }
        @Override public View getView(MainActivity a) { return null; }
        @Override public boolean hasLongView() { return false; }
        @Override public boolean hasEvent() { return true; }
        @Override public Drawable getIcon(Context c) { return null; }
        @Override public boolean isSubItem() { return false; }
        @Override public boolean viewIsRecyclable() { return true; }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                Intent intent;
                if (timerSeconds > 0) {
                    intent = new Intent(AlarmClock.ACTION_SET_TIMER);
                    intent.putExtra(AlarmClock.EXTRA_LENGTH, timerSeconds);
                    intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
                } else {
                    intent = new Intent(AlarmClock.ACTION_SET_ALARM);
                    intent.putExtra(AlarmClock.EXTRA_HOUR, alarmHour);
                    intent.putExtra(AlarmClock.EXTRA_MINUTES, alarmMinute);
                    intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                activity.finishIfNotHome();
            };
        }
    }
}
