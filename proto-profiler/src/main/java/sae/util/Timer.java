package sae.util;

import java.util.Arrays;

public class Timer implements Comparable<Timer> {

	public static final String UNIT_NANO_SECONDS = "ns";
	
	public static final String UNIT_MILLI_SECONDS = "ms";
	
	public static final String UNIT_SECONDS = "s";
	
	private final long start;

	private long end;

	public Timer() {
		this.start = System.nanoTime();
	}

	/**
	 * Constructs a timer for a given period.
	 * Used e.g. to construct the mean, or median
	 */
	private Timer(long start, long stop) {
		this.start = 0;
		this.end = stop;
	}
	
	/**
	 * stop the timer
	 */
	public void stop() {
		this.end = System.nanoTime();
	}

	private boolean finished() {
		if(start == 0)
			return true;
		return end > 0;
	}

	/**
	 * @return the time in nano seconds elapsed since the start of this timer
	 */
	public long elapsedNanoSeconds() {
		if (!finished())
			return System.nanoTime() - start;
		return end - start;
	}

	/**
	 * @return the time in nano seconds elapsed since the start of this timer
	 */
	public double elapsedMilliSeconds() {
		return nanoToMiliSeconds(elapsedNanoSeconds());
	}

	/**
	 * @return the time in nano seconds elapsed since the start of this timer
	 */
	public double elapsedSeconds() {
		return nanoToSeconds(elapsedNanoSeconds());
	}

	private static String asStringWithUnits(long value, String unit) {
		return String.valueOf(value) + " (" + unit + ")";
	}
	
	private static String asStringWithUnits(double value, String unit) {
		return String.valueOf(value) + " (" + unit + ")";		
	}

	
	/**
	 * @return the time in nano seconds elapsed since the start of this timer as a string.
	 * The string contains in the unit in the form "value (ns)"
	 * 
	 */
	public String elapsedNanoSecondsWithUnit() {
		return asStringWithUnits(elapsedNanoSeconds(),UNIT_NANO_SECONDS);
	}

	/**
	 * @return the time in nano seconds elapsed since the start of this timer
	 */
	public String elapsedMilliSecondsWithUnit() {
		return asStringWithUnits(elapsedMilliSeconds(),UNIT_MILLI_SECONDS);

	}

	/**
	 * @return the time in nano seconds elapsed since the start of this timer
	 */
	public String elapsedSecondsWithUnit() {
		return asStringWithUnits(elapsedSeconds(),UNIT_SECONDS);
	}
	
	/**
	 * @param timers
	 * @return
	 */
	public static Timer mean(Timer[] timers) {
		if (timers.length == 0)
			return new ValueTimer(0);
		long mean = timers[0].elapsedNanoSeconds();
		for (int i = 1; i < timers.length; i++) {
			Timer timer = timers[i];
			mean += timer.elapsedNanoSeconds();
			mean /= 2;
		}
		return new ValueTimer(mean);
	}

	
	/**
	 * @param timers
	 * @return
	 */
	public static Timer median(Timer[] timers) {
		if (timers.length == 0)
			return new ValueTimer(0);
		if (timers.length == 1)
			return timers[0];
		Arrays.sort(timers);
		
		int mid = timers.length / 2;
		if (timers.length % 2 == 0)
			return timers[mid];

		return new ValueTimer( (timers[mid].elapsedNanoSeconds() + timers[mid + 1].elapsedNanoSeconds()) / 2 );
	}
	
	
	/**
	 * @param timers
	 * @return
	 */
	public static Timer max(Timer[] timers) {
		if (timers.length == 0)
			return new ValueTimer(0);
		if (timers.length == 1)
			return timers[0];
		Arrays.sort(timers);

		return timers[timers.length - 1];
	}
	
	/**
	 * @param timers
	 * @return
	 */
	public static Timer min(Timer[] timers) {
		if (timers.length == 0)
			return new ValueTimer(0);
		if (timers.length == 1)
			return timers[0];
		Arrays.sort(timers);

		return timers[0];
	}

	public static double nanoToSeconds(long nanos) {
		return (double) nanos / 1000000000;
	}

	public static double nanoToMiliSeconds(long nanos) {
		return (double) nanos / 1000000;
	}

	public static double nanoToSeconds(double nanos) {
		return nanos / 1000000000;
	}

	public static double nanoToMiliSeconds(double nanos) {
		return nanos / 1000000;
	}

	
	private static class ValueTimer extends Timer {

		/**
		 * Constructs a timer with a given value.
		 * Used e.g. to construct the mean, or median
		 * @param stop
		 */
		private ValueTimer(long stop) {
			super(0, stop);
		}

		/* (non-Javadoc)
		 * @see sae.util.Timer#stop()
		 */
		@Override
		public void stop() {
			// do nothing, this timer was never meant to run
		}
		
	}


	@Override
	public int compareTo(Timer o) {
		if( !this.finished() )
			throw new IllegalStateException("Trying to compare a running timer: " + this);
		if( !o.finished() )
			throw new IllegalStateException("Trying to compare a running timer: " + o);
		// FIXME might be an unsafe operation, but I expect the difference not to be great
		return (int)(this.elapsedNanoSeconds() - o.elapsedNanoSeconds());
	}
}