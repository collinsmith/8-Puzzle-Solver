package edu.csupomona.cs.cs420.project1;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
		0, 1, 2, 3, 4, 5, 6, 7, 8
	};
	
	private static final int GOAL_HASH = Arrays.hashCode(GOAL_STATE);

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
		int misplacedTiles = 0;
		for (int j = 0; j < puzzle.length; j++) {
			if (puzzle[j] == 0) {
				continue;
			}

			if (puzzle[j] != GOAL_STATE[j]) {
				misplacedTiles++;
			}
		}

		return misplacedTiles;
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
		System.out.format(" 1. generate random 8-puzzle problems%n");
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

		Path file = Paths.get(".", "output", "output.txt");
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(file, charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			System.out.format("%3s %16s %16s %16s %16s%n", "d", "h1 cost", "h1 time", "h2 cost", "h2 time");
			writer.write(String.format("%s\t%s\t%s\t%s\t%s%n", "d", "h1 cost", "h1 time", "h2 cost", "h2 time"));

			int numDifferentDepths = 0;

			byte[] puzzle;
			for (int i = 0; i < iterations; i++) {
				do {
					puzzle = generateRandomPuzzle();
				} while (!isSolvable(puzzle));

				Result r1 = search(puzzle, h1);
				Result r2 = search(puzzle, h2);

				assert r1.DEPTH == r2.DEPTH : "Path lengths are not equal!";
				if (r1.DEPTH != r2.DEPTH) {
					numDifferentDepths++;
					System.out.format("depths not equal (%d:%d)%n", r1.DEPTH, r2.DEPTH);
				}
				
				System.out.format("%3d %16s %16s %16s %16s%n", r1.DEPTH, r1.NODES_GENERATED, TimeUnit.NANOSECONDS.toMillis(r1.END_TIME-r1.START_TIME), r2.NODES_GENERATED, TimeUnit.NANOSECONDS.toMillis(r2.END_TIME-r2.START_TIME));
				writer.write(String.format("%d\t%d\t%d\t%d\t%d%n", r1.DEPTH, r1.NODES_GENERATED, TimeUnit.NANOSECONDS.toMillis(r1.END_TIME-r1.START_TIME), r2.NODES_GENERATED, TimeUnit.NANOSECONDS.toMillis(r2.END_TIME-r2.START_TIME)));
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
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

		byte[] puzzle = new byte[GOAL_STATE.length];
		for (int i = 0; i < puzzle.length; i++) {
			puzzle[i] = SCAN.nextByte();
		}

		if (!isSolvable(puzzle)) {
			System.out.format("This puzzle is not solvable.%n");
		} else {
			System.out.format("Puzzle accepted, solving...%n");

			Result r1 = search(puzzle, h1);
			Result r2 = search(puzzle, h2);

			assert r1.DEPTH == r2.DEPTH : "Path lengths are not equal!";
			System.out.format("Solution depth = %d%n", r1.DEPTH);
			System.out.format("Displaying path...%n");
			printPath(r1.PATH);
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

	private static Result search(byte[] puzzle, Heuristic h) {
		Set<Node> explored = new HashSet<>();
		PriorityQueue<Node> frontier = new PriorityQueue<>((n1, n2) -> {
			return (n1.COST + h.evaluate(n1.PUZZLE)) - (n2.COST + h.evaluate(n2.PUZZLE));
		});

		Node n;
		int successorCost;
		int nodesGenerated = 0;
		boolean frontierContainsSuccessor;
		frontier.offer(new Node(puzzle));
		final long START_TIME = System.nanoTime();
		while (!frontier.isEmpty()) {
			n = frontier.poll();
			if (n.hashCode() == GOAL_HASH && isGoal(n.PUZZLE)) {
				return new Result(
					buildPath(new LinkedList<>(), n),
					nodesGenerated,
					START_TIME,
					System.nanoTime()
				);
			}

			for (Node successor : n.generateSuccessors()) {
				if (explored.contains(successor)) {
					continue;
				}

				nodesGenerated++;
				successorCost = n.COST + h.evaluate(successor.PUZZLE);
				frontierContainsSuccessor = frontier.contains(successor);
				if (!frontierContainsSuccessor || successorCost < successor.COST) {
					successor.PARENT = n;
					successor.COST = successorCost;
					if (frontierContainsSuccessor) {
						frontier.remove(successor);
					}

					frontier.offer(successor);
				}
			}

			explored.add(n);
		}

		return new Result(
			Collections.EMPTY_LIST,
			nodesGenerated,
			START_TIME,
			System.nanoTime()
		);
	}

	private static List<Node> buildPath(List<Node> l, Node n) {
		if (n == null) {
			return l;
		} else {
			l.add(0, n);
			return buildPath(l, n.PARENT);
		}
	}

	@FunctionalInterface
	private static interface Heuristic {
		int evaluate(byte[] puzzle);
	}

	private static class Node {
		final byte[] PUZZLE;
		final int BLANK_INDEX;
		final int HASH;

		int COST;
		Node PARENT;

		Node(byte[] puzzle) {
			this(puzzle, findBlankIndex(puzzle), null, 0);
		}

		Node(byte[] puzzle, int blankIndex, Node parent, int cost) {
			this.PUZZLE = puzzle;
			this.BLANK_INDEX = blankIndex;
			this.HASH = Arrays.hashCode(PUZZLE);
			
			this.PARENT = parent;
			this.COST = cost;
		}

		@Override
		public int hashCode() {
			return HASH;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}

			if (!(obj instanceof Node)) {
				return false;
			}

			Node n = (Node)obj;
			return Arrays.equals(PUZZLE, n.PUZZLE);
		}

		Set<Node> generateSuccessors() {
			int newBlankIndex;
			byte[] successor;
			Set<Node> successors = new HashSet<>();
			int actions = AVAIL_ACTIONS[BLANK_INDEX];

			if ((actions&ACTION_UP) != 0) {
				newBlankIndex = BLANK_INDEX-3;
				successor = Arrays.copyOf(PUZZLE, PUZZLE.length);
				swap(successor, BLANK_INDEX, newBlankIndex);
				successors.add(new Node(successor, newBlankIndex, this, Integer.MAX_VALUE));
			}

			if ((actions&ACTION_DOWN) != 0) {
				newBlankIndex = BLANK_INDEX+3;
				successor = Arrays.copyOf(PUZZLE, PUZZLE.length);
				swap(successor, BLANK_INDEX, newBlankIndex);
				successors.add(new Node(successor, newBlankIndex, this, Integer.MAX_VALUE));
			}

			if ((actions&ACTION_LEFT) != 0) {
				newBlankIndex = BLANK_INDEX-1;
				successor = Arrays.copyOf(PUZZLE, PUZZLE.length);
				swap(successor, BLANK_INDEX, newBlankIndex);
				successors.add(new Node(successor, newBlankIndex, this, Integer.MAX_VALUE));
			}


			if ((actions&ACTION_RIGHT) != 0) {
				newBlankIndex = BLANK_INDEX+1;
				successor = Arrays.copyOf(PUZZLE, PUZZLE.length);
				swap(successor, BLANK_INDEX, newBlankIndex);
				successors.add(new Node(successor, newBlankIndex, this, Integer.MAX_VALUE));
			}

			return successors;
		}
	}

	private static class Result {
		final List<Node> PATH;
		final int DEPTH;
		final int NODES_GENERATED;
		final long START_TIME;
		final long END_TIME;

		Result(List<Node> path, int nodesGenerated, long startTime, long endTime) {
			this.PATH = path;
			this.DEPTH = path.size()-1;
			this.NODES_GENERATED = nodesGenerated;
			this.START_TIME = startTime;
			this.END_TIME = endTime;
		}
	}
}
