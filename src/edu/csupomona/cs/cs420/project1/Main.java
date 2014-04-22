package edu.csupomona.cs.cs420.project1;

import java.util.Random;
import java.util.Scanner;

public class Main {
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
			case 1:	return generateRandomPuzzle();
			case 2:	return enterPuzzleManually();
			case 3:	return false;
			default:	return displayMenu();
		}
	}

	private static boolean generateRandomPuzzle() {
		System.out.format("A random puzzle has been generated.%n");

		int temp;
		int randomIndex;
		int[] puzzle = Puzzle.copyGoalState();
		for (int i = 0; i < puzzle.length; i++) {
			randomIndex = RAND.nextInt(puzzle.length);
			swap(puzzle, i, randomIndex);
		}

		return true;
	}

	private static boolean enterPuzzleManually() {
		System.out.format("You may begin entering your puzzle.%n");

		int[] puzzle = new int[9];
		for (int i = 0; i < puzzle.length; i++) {
			puzzle[i] = SCAN.nextInt();
		}

		return true;
	}

	private static void swap(int[] array, int i, int j) {
		if (i == j) {
			return;
		}

		array[i] ^= array[j];
		array[j] ^= array[i];
		array[i] ^= array[j];
	}

	private static void printPuzzle(int[] puzzle) {
		for (int i = 0; i < puzzle.length; i++) {
			System.out.format("%d ", puzzle[i]);
			if (i == 2 || i == 5 || i == 8) {
				System.out.format("%n");
			}
		}
	}
}
