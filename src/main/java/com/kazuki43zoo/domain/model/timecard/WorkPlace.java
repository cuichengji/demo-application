package com.kazuki43zoo.domain.model.timecard;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Data
public class WorkPlace implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final LocalDate BASE_DATE = new LocalDate(0);

    public static final String MAIN_OFFICE_UUID = "00000000-0000-0000-0000-000000000000";

    private String workPlaceUuid;

    private String workPlaceName;

    private String workPlaceNameJa;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime baseBeginTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime baseFinishTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "H:mm")
    private LocalTime unitTime;

    private List<BreakTime> breakTimes;

    @lombok.Setter(lombok.AccessLevel.NONE)
    private transient int baseWorkTimeMinute;

    @lombok.Setter(lombok.AccessLevel.NONE)
    @lombok.Getter(lombok.AccessLevel.NONE)
    private transient Interval baseWorkTimeInterval;

    @lombok.Setter(lombok.AccessLevel.NONE)
    @lombok.Getter(lombok.AccessLevel.NONE)
    private transient List<Interval> breakTimeIntervals;

    public void initialize() {
        final List<Interval> breakTimeIntervals = new ArrayList<>();
        if (getBreakTimes() != null) {
            for (BreakTime breakTime : getBreakTimes()) {
                breakTimeIntervals.addAll(breakTime.toBreakTimeIntervals());
            }
            Collections.sort(breakTimeIntervals, new Comparator<Interval>() {
                @Override
                public int compare(Interval o1, Interval o2) {
                    return o1.getStart().compareTo(o2.getStart());
                }
            });
        }
        this.breakTimeIntervals = breakTimeIntervals;
        this.baseWorkTimeInterval = new Interval(BASE_DATE.toDateTime(this.baseBeginTime),
                BASE_DATE.toDateTime(this.baseFinishTime));
        this.baseWorkTimeMinute = (int) toMinute(this.baseWorkTimeInterval)
                - calculateContainsBreakTimeMinute(this.baseWorkTimeInterval);
    }

    public int calculateWorkingMinute(final Interval workTimeInterval, final WorkPlace mainOffice) {
        int workingMinute = (int) toMinute(workTimeInterval) - calculateContainsBreakTimeMinute(workTimeInterval);
        return truncateWithTimeUnit(workingMinute, mainOffice);
    }

    public int truncateWithTimeUnit(final int minute) {
        return truncateWithTimeUnit(minute, this);
    }

    public int truncateWithTimeUnit(final int minute, final WorkPlace mainOffice) {
        int determinedMinute = 0;
        int undeterminedMinute = minute;
        int baseWorkTimeMinute = this.baseWorkTimeMinute;
        if (baseWorkTimeMinute < mainOffice.getBaseWorkTimeMinute()
                && mainOffice.getBaseWorkTimeMinute() <= minute) {
            baseWorkTimeMinute = mainOffice.getBaseWorkTimeMinute();
        }
        if (baseWorkTimeMinute <= minute) {
            determinedMinute = baseWorkTimeMinute;
            undeterminedMinute = minute - baseWorkTimeMinute;
        }
        final int unitTimeMinute = (getUnitTime().getHourOfDay() * 60)
                + getUnitTime().getMinuteOfHour();
        return determinedMinute + undeterminedMinute - (undeterminedMinute % unitTimeMinute);
    }

    public boolean isTardyOrEarlyLeaving(final DateTime beginTime, final DateTime finishTime) {
        if (beginTime != null && beginTime.isAfter(this.baseWorkTimeInterval.getStart())) {
            return true;
        }
        if (finishTime != null && finishTime.isBefore(this.baseWorkTimeInterval.getEnd())) {
            return true;
        }
        return false;
    }

    public int calculateContainsBreakTimeMinute(final Interval workTimeInterval) {
        if (getBreakTimes() == null) {
            return 0;
        }
        long minute = 0;
        for (final Interval breakTimeInterval : this.breakTimeIntervals) {
            if (workTimeInterval.overlaps(breakTimeInterval)) {
                minute += toMinute(workTimeInterval.overlap(breakTimeInterval));
            }
        }
        return (int) minute;
    }

    private long toMinute(final Interval interval) {
        return TimeUnit.MILLISECONDS.toMinutes(interval.toDuration().getMillis());
    }

}
