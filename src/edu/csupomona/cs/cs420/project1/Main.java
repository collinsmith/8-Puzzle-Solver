package edu.csupomona.cs.cs420.project1;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class Main {
	private static final byte ACTION_NOOP	= 0;
	private static final byte ACTION_UP		= 1<<0;
	private static final byte ACTION_DOWN	= 1<<1;
	private static final byte ACTION_LEFT	= 1<<2;
	private static final byte ACTION_RIGHT	= 1<<3;

	private static final byte[] AVAIL_ACTIONS = {
		ACTION_DOWN|ACTION_RIGHT,			ACTION_LEFT|ACTION_DOWN|ACTION_RIGHT,			ACTION_LEFT|ACTION_DOWN,
		ACTION_UP|ACTION_DOWN|ACTION_RIGHT,		ACTION_LEFT|ACTION_UP|ACTION_DOWN|ACTION_RIGHT,		ACTION_LEFT|ACTION_UP|ACTION_DOWN,
		ACTION_UP|ACTION_RIGHT,				ACTION_LEFT|ACTION_UP|ACTION_RIGHT,				ACTION_LEFT|ACTION_UP
	};

	private static final byte[] GOAL_STATE = {
		0, 1, 2,
		3, 4, 5,
		6, 7, 8
	};

	private static final byte[][] MANHATTAN_DIST = {
		{0, 1, 2, 1, 2, 3, 2, 3, 4}, // 0
		{1, 0, 1, 2, 1, 2, 3, 2, 3}, // 1
		{2, 1, 0, 3, 2, 1, 4, 3, 2}, // 2
		{1, 2, 3, 0, 1, 2, 1, 2, 3}, // 3
		{2, 1, 2, 1, 0, 1, 2, 1, 2}, // 4
		{3, 2, 1, 2, 1, 0, 3, 2, 1}, // 5
		{2, 3, 4, 1, 2, 3, 0, 1, 2}, // 6
		{3, 2, 3, 2, 1, 2, 1, 0, 1}, // 7
		{4, 3, 2, 3, 2, 1, 2, 1, 0}  // 8
	};

	private static final Heuristic h1 = (puzzle) -> {
		int numBlocksOutOfPlace = 0;
		for (int j = 0; j < puzzle.length; j++) {
			if (puzzle[j] != GOAL_STATE[j]) {
				numBlocksOutOfPlace++;
			}
		}

		return numBlocksOutOfPlace;
	};

	private static final Heuristic h2 = (puzzle) -> {
		int cumulativeDistance = 0;
		for (int j = 0; j < puzzle.length; j++) {
			if (puzzle[j] == 0) {
				continue;
			}

			cumulativeDistance += MANHATTAN_DIST[j][puzzle[j]];
		}

		return cumulativeDistance;
	};

	private static Scanner SCAN;
	private static Random RAND;

	public static void main(String[] args) {
		SCAN = new Scanner(System.in);
		RAND = new Random();

		do {
		} while (displayMenu());
	}

	private static boolean displayMenu() {
		System.out.format("Select an option:%n");
		System.out.format(" 1. generate a random 8-puzzle problem%n");
		System.out.format(" 2. enter an 8-puzzle problem manually%n");
		System.out.format(" 3. exit%n");
		System.out.format("-> ");

		int opt = SCAN.nextInt();
		switch (opt) {
			case 1:	return generateRandomPuzzles();
			case 2:	return enterPuzzleManually();
			case 3:	return false;
			default:	return displayMenu();
		}
	}

	private static boolean generateRandomPuzzles() {
		System.out.format("How many random iterations do you want to generate? ");
		int iterations = SCAN.nextInt();

		byte[] puzzle;
		for (int i = 0; i < iterations; i++) {
			do {
				puzzle = generateRandomPuzzle();
			} while (!isSolvable(puzzle));

			search(puzzle, h1);
			search(puzzle, h2);
		}

		return true;
	}

	private static byte[] generateRandomPuzzle() {
		int temp;
		int randomIndex;
		byte[] puzzle = copyGoal();
		for (int i = 0; i < puzzle.length; i++) {
			randomIndex = RAND.nextInt(puzzle.length);
			swap(puzzle, i, randomIndex);
		}

		return puzzle;
	}

	private static boolean enterPuzzleManually() {
		System.out.format("You may begin entering your puzzle.%n");

		byte[] puzzle = new byte[9];
		for (int i = 0; i < puzzle.length; i++) {
			puzzle[i] = SCAN.nextByte();
		}

		if (!isSolvable(puzzle)) {
			System.out.format("This puzzle is not solvable.%n");
		} else {
			System.out.format("Puzzle accepted, solving...%n");
			List<byte[]> p1 = search(puzzle, h1);
			List<byte[]> p2 = search(puzzle, h2);
			if (p1.size() != p2.size()) {
				System.out.format("We have a big problem!%n");
			}

			System.out.format("The solution depth is %d%n", p1.size()-1);
		}

		return true;
	}

	private static void swap(byte[] array, int i, int j) {
		if (i == j) {
			return;
		}

		array[i] ^= array[j];
		array[j] ^= array[i];
		array[i] ^= array[j];
	}

	private static void printPuzzle(byte[] puzzle) {
		for (int i = 0; i < puzzle.length; i++) {
			System.out.format("%d ", puzzle[i]);
			switch (i) {
				case 2:
				case 5:
				case 8:
					System.out.format("%n");
					break;
			}
		}
	}

	private static void printPath(List<Node> sequence) {
		sequence.stream().forEach((n) -> {
			printPuzzle(n.PUZZLE);
			System.out.format("%n");
		});
	}

	private static int getNumInversions(byte[] puzzle) {
		int inversions = 0;
		for (int i = 0; i < puzzle.length; i++) {
			if (puzzle[i] == 0) {
				continue;
			}

			for (int j = i; j < puzzle.length; j++) {
				if (puzzle[j] == 0) {
					continue;
				}

				if (puzzle[j] < puzzle[i]) {
					inversions++;
				}
			}
		}

		return inversions;
	}

	private static boolean isSolvable(byte[] puzzle) {
		return (getNumInversions(puzzle)&1) == 0;
	}

	private static byte[] copyGoal() {
		return Arrays.copyOf(GOAL_STATE, GOAL_STATE.length);
	}

	private static boolean isGoal(byte[] puzzle) {
		return Arrays.equals(GOAL_STATE, puzzle);
	}

	private static int findBlankIndex(byte[] puzzle) {
		for (int i = 0; i < puzzle.length; i++) {
			if (puzzle[i] == 0) {
				return i;
			}
		}

		return -1;
	}

	private static List<byte[]> search(byte[] puzzle, Heuristic h) {
		Set<byte[]> explored = new HashSet<>();
		Map<byte[], byte[]> map = new HashMap<>();
		PriorityQueue<byte[]> frontier = new PriorityQueue<>((n1, n2) -> {
			return h.evaluate(n1) - h.evaluate(n2);
		});

		int cost = 0;
		int nodesGenerated = 0;
		frontier.offer(puzzle);
		while (!frontier.isEmpty()) {
			System.out.println("new loop");
			byte[] n = frontier.poll();
			if (isGoal(n)) {
				System.out.format("%d nodes generated.%n", nodesGenerated);
				return buildPath(new LinkedList<>(), map, n);
			}

			explored.add(n);
			System.out.println(explored.size());
			Set<byte[]> sucessors = Node.generateSuccessors(n);
			System.out.println("Successors = " + sucessors.size());
			for (byte[] successor : sucessors) {
				if (explored.contains(successor)) {
					System.out.println("skipping puzzle");
					continue;
				}

				nodesGenerated++;
				map.put(successor, n);
				//int g_score = cost + 1;
				boolean frontierContainsSuccessor = frontier.contains(successor);
				if (!frontierContainsSuccessor) {// || g_score < cost) {
					//int f_score = g_score + h.evaluate(successor.PUZZLE);
					//if (!frontierContainsSuccessor) {
						frontier.add(successor);
						System.out.println("adding to frontier");
					//}
				}
			}
		}

		return new LinkedList<>();
	}

	private static List<byte[]> buildPath(List<byte[]> l, Map<byte[], byte[]> map, byte[] n) {
		if (n == null) {
			return l;
		} else {
			l.add(0, n);
			return buildPath(l, map, map.get(n));
		}
	}

	@FunctionalInterface
	private static interface Heuristic {
		int evaluate(byte[] puzzle);
	}

	private static class Node {
		private final Node PARENT;
		private final byte[] PUZZLE;

		Node(byte[] puzzle) {
			this(null, puzzle);
		}

		Node(Node parent, byte[] puzzle) {
			this.PARENT = parent;
			this.PUZZLE = puzzle;
		}

		static Set<byte[]> generateSuccessors(byte[] puzzle) {
			int new_index_of_blank;
			byte[] successorPuzzle;
			Set<byte[]> successors = new HashSet<>();
			int blank_index = findBlankIndex(puzzle);
			int actions = AVAIL_ACTIONS[blank_index];

			if ((actions&ACTION_UP) != 0) {
				new_index_of_blank = blank_index-3;
				successorPuzzle = Arrays.copyOf(puzzle, puzzle.length);
				swap(successorPuzzle, blank_index, new_index_of_blank);
				successors.add(successorPuzzle);
			}

			if ((actions&ACTION_DOWN) != 0) {
				new_index_of_blank = blank_index+3;
				successorPuzzle = Arrays.copyOf(puzzle, puzzle.length);
				swap(successorPuzzle, blank_index, new_index_of_blank);
				successors.add(successorPuzzle);
			}

			if ((actions&ACTION_LEFT) != 0) {
				new_index_of_blank = blank_index-1;
				successorPuzzle = Arrays.copyOf(puzzle, puzzle.length);
				swap(successorPuzzle, blank_index, new_index_of_blank);
				successors.add(successorPuzzle);
			}


			if ((actions&ACTION_RIGHT) != 0) {
				new_index_of_blank = blank_index+1;
				successorPuzzle = Arrays.copyOf(puzzle, puzzle.length);
				swap(successorPuzzle, blank_index, new_index_of_blank);
				successors.add(successorPuzzle);
			}

			return successors;
		}
	}
}
