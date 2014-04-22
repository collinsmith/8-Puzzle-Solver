package edu.csupomona.cs.cs420.project1;

import java.util.Arrays;

public class Puzzle {
	private static final int[] GOAL_STATE = {
		0, 1, 2,
		3, 4, 5,
		6, 7, 8
	};

	public static final int[] copyGoalState() {
		return Arrays.copyOf(GOAL_STATE, GOAL_STATE.length);
	}

	private final int[] puzzle;

	public Puzzle() {
		//...
	}
}
