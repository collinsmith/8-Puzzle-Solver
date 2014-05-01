package edu.csupomona.cs.cs420.project1;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 8-puzzle solver which can either take an 8-puzzle as input and solve it while
 * displaying the steps, or solve a specified number of randomly generated
 * 8-puzzle problems and output the results.
 *
 * @author Collin Smith <collinsmith@csupomona.edu>
 */
public class Main {
	/**
	 * Constant which represents the no-op bit.
	 */
	private static final byte ACTION_NOOP	= 0;

	/**
	 * Constant which represents the "move up" bit.
	 */
	private static final byte ACTION_UP		= 1<<0;

	/**
	 * Constant which represents the "move down" bit.
	 */
	private static final byte ACTION_DOWN	= 1<<1;

	/**
	 * Constant which represents the "move left" bit.
	 */
	private static final byte ACTION_LEFT	= 1<<2;

	/**
	 * Constant which represents the "move right" bit.
	 */
	private static final byte ACTION_RIGHT	= 1<<3;

	/**
	 * Table which keeps bitsums for each location within the puzzle for the
	 * actions available at those locations.
	 */
	private static final byte[] AVAIL_ACTIONS = {
		ACTION_DOWN|ACTION_RIGHT,			ACTION_LEFT|ACTION_DOWN|ACTION_RIGHT,			ACTION_LEFT|ACTION_DOWN,
		ACTION_UP|ACTION_DOWN|ACTION_RIGHT,		ACTION_LEFT|ACTION_UP|ACTION_DOWN|ACTION_RIGHT,		ACTION_LEFT|ACTION_UP|ACTION_DOWN,
		ACTION_UP|ACTION_RIGHT,				ACTION_LEFT|ACTION_UP|ACTION_RIGHT,				ACTION_LEFT|ACTION_UP
	};

	/**
	 * Array which represents the position of the elements within the goal
	 * state.
	 */
	private static final byte[] GOAL_STATE = {
		0, 1, 2, 3, 4, 5, 6, 7, 8
	};

	/**
	 * Constant which represents the hashed version of the goal state. This is
	 * only used to help save computation time to try and avoid most state-
	 * goal state comparisons.
	 */
	private static final int GOAL_HASH = Arrays.hashCode(GOAL_STATE);

	/**
	 * Table which keeps arrays that store the distances from a location to
	 * where that piece needs to be. This allows O(1) lookup of all distances.
	 */
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

	/**
	 * Heuristic which returns the Hamming distance, or the number of
	 * misplaced tiles, except the "blank" tile.
	 */
	private static final Heuristic<byte[]> h1 = (puzzle) -> {
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

	/**
	 * Heuristic which returns the sum of the Manhattan distances, except
	 * the "blank" tile.
	 */
	private static final Heuristic<byte[]> h2 = (puzzle) -> {
		int cumulativeDistance = 0;
		for (int j = 0; j < puzzle.length; j++) {
			if (puzzle[j] == 0) {
				continue;
			}

			cumulativeDistance += MANHATTAN_DIST[j][puzzle[j]];
		}

		return cumulativeDistance;
	};

	/**
	 * Scanner used for user input.
	 */
	private static Scanner SCAN;

	/**
	 * Random number generated used to randomize generated puzzles.
	 */
	private static Random RAND;

	/**
	 * Main method executed at program start which displays the menu.
	 *
	 * @param args arguments passed to the program (should be empty)
	 */
	public static void main(String[] args) {
		SCAN = new Scanner(System.in);
		RAND = new Random();

		do {
		} while (displayMenu());
	}

	/**
	 * Displays the main menu and performs actions based on user input.
	 *
	 * @return {@code false} if the menu was exited, otherwise {@code true}
	 */
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

	/**
	 * Asks the user to enter the number of random puzzles to generate and
	 * then solves those puzzles. Unsolvable puzzles will be ignored.
	 *
	 * @return {@code true}
	 */
	private static boolean generateRandomPuzzles() {
		System.out.format("How many random iterations do you want to generate? ");
		int iterations = SCAN.nextInt();

		Path file = Paths.get(".", "output", "output.txt");
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(file, charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			System.out.format("%-8s %-8s %-8s %-8s %-16s %-16s%n", "h1 d", "h2 d", "h1 cost", "h2 cost", "h1 time", "h2 time");
			writer.write(String.format("%s\t%s\t%s\t%s\t%s\t%s%n", "h1 d", "h2 d", "h1 cost", "h2 cost", "h1 time", "h2 time"));

			byte[] puzzle;
			for (int i = 0; i < iterations; i++) {
				do {
					puzzle = generateRandomPuzzle();
				} while (!isSolvable(puzzle));

				Result r1 = search(puzzle, h1);
				Result r2 = search(puzzle, h2);

				assert r1.DEPTH == r2.DEPTH : "Path lengths are not equal!";
				System.out.format("%-8d %-8d %-8s %-8s %-16s %-16s%n", r1.DEPTH, r2.DEPTH, r1.NODES_EXPANDED, r2.NODES_EXPANDED, toTimeUnit(r1.END_TIME-r1.START_TIME), toTimeUnit(r2.END_TIME-r2.START_TIME));
				writer.write(String.format("%d\t%d\t%d\t%d\t%d\t%d%n", r1.DEPTH, r2.DEPTH, r1.NODES_EXPANDED, r2.NODES_EXPANDED, toTimeUnit(r1.END_TIME-r1.START_TIME), toTimeUnit(r2.END_TIME-r2.START_TIME)));
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * Converts a given value in nanos to a value the user might be more
	 * comfortable with.
	 *
	 * @param nanos value to convert in nanos
	 *
	 * @return converted value in micro seconds
	 */
	private static long toTimeUnit(long nanos) {
		return TimeUnit.NANOSECONDS.toMicros(nanos);
	}

	/**
	 * Creates a new puzzle defaulted to {@link #GOAL_STATE} and then has each
	 * position randomly swapped with another iteratively.
	 *
	 * @return a randomly generated puzzle
	 */
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

	/**
	 * Asks the user to enter a puzzle of their choosing, which is then solved
	 * and returned with the depth and steps taken to solve.
	 *
	 * @return {@code true}
	 */
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

	/**
	 * Swaps two positions within an array.
	 *
	 * @param array the array to swap indeces in
	 * @param i a position
	 * @param j another position
	 */
	private static void swap(byte[] array, int i, int j) {
		if (i == j) {
			return;
		}

		array[i] ^= array[j];
		array[j] ^= array[i];
		array[i] ^= array[j];
	}

	/**
	 * Prints a given 3x3 puzzle.
	 *
	 * @param puzzle the puzzle to print
	 */
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

	/**
	 * Prints a solution path (a sequence of puzzles).
	 *
	 * @param sequence sequence of puzzles within the path
	 */
	private static void printPath(List<Node> sequence) {
		sequence.stream().forEach((n) -> {
			printPuzzle(n.PUZZLE);
			System.out.format("%n");
		});
	}

	/**
	 * Returns the number of inversions within a given puzzle.
	 *
	 * @param puzzle puzzle to returns the number of inversions for
	 *
	 * @return the number of inversions in that puzzle
	 */
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

	/**
	 * Returns whether or not a given puzzle is solvable.
	 *
	 * @param puzzle the puzzle to check
	 *
	 * @return {@code true} if the puzzle is solvable, otherwise {@code false}
	 */
	private static boolean isSolvable(byte[] puzzle) {
		return (getNumInversions(puzzle)&1) == 0;
	}

	/**
	 * Returns a copy of {@link #GOAL_STATE}.
	 *
	 * @return a copy of {@link #GOAL_STATE}
	 */
	private static byte[] copyGoal() {
		return Arrays.copyOf(GOAL_STATE, GOAL_STATE.length);
	}

	/**
	 * Returns whether or not a given puzzle is equal to {@link #GOAL_STATE}.
	 *
	 * @param puzzle the puzzle to check
	 *
	 * @return {@code true} if it is, otherwise {@code false}
	 */
	private static boolean isGoal(byte[] puzzle) {
		return Arrays.equals(GOAL_STATE, puzzle);
	}

	/**
	 * Iterates through a given puzzle and returns the location of the blank
	 * tile.
	 *
	 * @param puzzle the puzzle to search
	 *
	 * @return the index of the blank tile, or {@code -1} if there is none
	 */
	private static int findBlankIndex(byte[] puzzle) {
		for (int i = 0; i < puzzle.length; i++) {
			if (puzzle[i] == 0) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Searches for the path from a given puzzle to {@link #GOAL_STATE} using
	 * a specified heuristic to calculate the estimated distances remaining in
	 * each sub-state.
	 *
	 * @param puzzle initial state of the puzzle
	 * @param h heuristic to use when estimating sub-state distances remaining
	 *
	 * @return {@link Result} object which stores various information related
	 *	     to this search.
	 */
	private static Result search(byte[] puzzle, Heuristic<byte[]> h) {
		Set<Node> explored = new HashSet<>();
		PriorityQueue<Node> frontier = new PriorityQueue<>((n1, n2) -> {
			return n1.heuristicCost - n2.heuristicCost;
		});

		int nodesVisited = 0;
		int nodesExpanded = 0;

		int tentativeCost;
		boolean frontierContainsSuccessor;

		Node n = new Node(puzzle);
		n.actualCost = 0;
		n.heuristicCost = n.actualCost + h.evaluate(n.PUZZLE);

		frontier.offer(n);
		final long START_TIME = System.nanoTime();
		while (!frontier.isEmpty()) {
			nodesVisited++;
			n = frontier.poll();
			if (n.hashCode() == GOAL_HASH && isGoal(n.PUZZLE)) {
				return new Result(
					buildPath(new LinkedList<>(), n),
					nodesVisited,
					nodesExpanded,
					START_TIME
				);
			}

			explored.add(n);
			for (Node successor : n.generateSuccessors()) {
				if (explored.contains(successor)) {
					continue;
				}

				nodesExpanded++;
				tentativeCost = n.actualCost + 1;
				frontierContainsSuccessor = frontier.contains(successor);
				if (!frontierContainsSuccessor || tentativeCost < successor.actualCost) {
					if (successor.actualCost < Integer.MAX_VALUE && tentativeCost < successor.actualCost) {
						System.out.println("new node better");
					}

					successor.parent = n;
					successor.actualCost = tentativeCost;
					successor.heuristicCost = successor.actualCost + h.evaluate(successor.PUZZLE);
					if (frontierContainsSuccessor && tentativeCost < successor.actualCost) {
						System.out.println("removing");
						frontier.remove(successor);
					}

					frontier.offer(successor);
				}
			}
		}

		return new Result(
			Collections.EMPTY_LIST,
			nodesVisited,
			nodesExpanded,
			START_TIME
		);
	}

	/**
	 * Given a sequence of nodes and a node, this method will recursively
	 * build the path from an initial puzzle to the goal puzzle.
	 *
	 * @param l sequence of nodes
	 * @param n current node
	 *
	 * @return a sequence of nodes representing the path from the initial
	 *	     state to the goal state
	 */
	private static List<Node> buildPath(List<Node> l, Node n) {
		if (n == null) {
			return l;
		} else {
			l.add(0, n);
			return buildPath(l, n.parent);
		}
	}

	/**
	 * A heuristic is a function which given a state will return a best guess
	 * of the cost it will take to go from that state to the goal state.
	 *
	 * @param <E> type of the state to evaluate
	 */
	@FunctionalInterface
	private static interface Heuristic<E> {
		/**
		 * Evaluates a given puzzle and returns a best guess of the cost it
		 * will take to go from that state to the goal state.
		 *
		 * @param state starting state
		 *
		 * @return integer which represents the best guess cost to go to the
		 *	     goal state
		 */
		int evaluate(E state);
	}

	/**
	 * A node is used within an A* algorithm to store state information. More
	 * specifically, this node is represented by a unique puzzle
	 * configuration, as well as the parent of this node, cost thus far to
	 * reach this node, and the estimated cost it will take from the initial
	 * node to this node and from this node to the goal state.
	 */
	private static class Node {
		/**
		 * Hashed value of the puzzle.
		 */
		final int HASH;

		/**
		 * Puzzle this node represents.
		 */
		final byte[] PUZZLE;

		/**
		 * Index of the blank tile.
		 */
		final int BLANK_INDEX;

		/**
		 * Parent of this node.
		 */
		Node parent;

		/**
		 * Cost thus far to reach this tile.
		 */
		int actualCost;

		/**
		 * {@code {@link #actualCost} + estimated remaining cost to the goal
		 * state}
		 */
		int heuristicCost;

		/**
		 * Constructs a node with a given puzzle and finds the index of the
		 * blank tile.
		 *
		 * @param puzzle puzzle this node represents
		 */
		Node(byte[] puzzle) {
			this(puzzle, findBlankIndex(puzzle));
		}

		/**
		 * Constructs a node with a given puzzle and the index of the blank
		 * tile within this puzzle.
		 *
		 * @param puzzle puzzle this node represents
		 * @param blankIndex index of the blank tile
		 */
		Node(byte[] puzzle, int blankIndex) {
			this.PUZZLE = puzzle;
			this.BLANK_INDEX = blankIndex;
			this.HASH = Arrays.hashCode(PUZZLE);

			this.parent = null;
			this.actualCost = Integer.MAX_VALUE;
			this.heuristicCost = Integer.MAX_VALUE;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			return HASH;
		}

		/**
		 * {@inheritDoc}
		 */
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

		/**
		 * Generates a collection of successors from this state.
		 *
		 * @return the child successors from this state
		 */
		Collection<Node> generateSuccessors() {
			int newBlankIndex;
			byte[] successor;
			Collection<Node> successors = new LinkedList<>();
			int actions = AVAIL_ACTIONS[BLANK_INDEX];

			if ((actions&ACTION_UP) != 0) {
				newBlankIndex = BLANK_INDEX-3;
				successor = Arrays.copyOf(PUZZLE, PUZZLE.length);
				swap(successor, BLANK_INDEX, newBlankIndex);
				successors.add(new Node(successor, newBlankIndex));
			}

			if ((actions&ACTION_DOWN) != 0) {
				newBlankIndex = BLANK_INDEX+3;
				successor = Arrays.copyOf(PUZZLE, PUZZLE.length);
				swap(successor, BLANK_INDEX, newBlankIndex);
				successors.add(new Node(successor, newBlankIndex));
			}

			if ((actions&ACTION_LEFT) != 0) {
				newBlankIndex = BLANK_INDEX-1;
				successor = Arrays.copyOf(PUZZLE, PUZZLE.length);
				swap(successor, BLANK_INDEX, newBlankIndex);
				successors.add(new Node(successor, newBlankIndex));
			}


			if ((actions&ACTION_RIGHT) != 0) {
				newBlankIndex = BLANK_INDEX+1;
				successor = Arrays.copyOf(PUZZLE, PUZZLE.length);
				swap(successor, BLANK_INDEX, newBlankIndex);
				successors.add(new Node(successor, newBlankIndex));
			}

			return successors;
		}
	}

	/**
	 * A result encapsulates information given by {@link Main#search(byte[], edu.csupomona.cs.cs420.project1.Main.Heuristic)}.
	 */
	private static class Result {
		/**
		 * Depth of the solution {@code path.size() - 1}.
		 */
		final int DEPTH;

		/**
		 * Path of the solution.
		 */
		final List<Node> PATH;

		/**
		 * Number of nodes visited in the search.
		 */
		final int NODES_VISITED;

		/**
		 * Number of successors generated.
		 */
		final int NODES_EXPANDED;

		/**
		 * Start time of the search.
		 */
		final long START_TIME;

		/**
		 * End time of the search.
		 */
		final long END_TIME;

		/**
		 * Constructors which initializes a Result object.
		 *
		 * @param path {@link #PATH}
		 * @param nodesVisited {@link #NODES_VISITED}
		 * @param nodesExpanded {@link #NODES_EXPANDED}
		 * @param startTime {@link #START_TIME}
		 */
		Result(List<Node> path, int nodesVisited, int nodesExpanded, long startTime) {
			this.PATH = path;
			this.DEPTH = path.size()-1;
			this.NODES_VISITED = nodesVisited;
			this.NODES_EXPANDED = nodesExpanded;
			this.START_TIME = startTime;
			this.END_TIME = System.nanoTime();
		}
	}
}
