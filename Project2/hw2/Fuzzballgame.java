package hw2;

public class FuzzballGame {
	public static final int MAX_BALLS = 4;
	public static final int MAX_OUTS = 3;
	public static final int MAX_STRIKES = 3;
	private static final int BASES_MASK = 0x07;
	private static final int BALLS_SHIFT = 3;
	private static final int STRIKES_SHIFT = 5;
	private static final int OUTS_SHIFT = 7;
	private static final int INNING_SHIFT = 9;
	private static final int TOP_OF_INNING_FLAG = 0x100;
	public static final int INNING_MASK = 0x1F;

	private int gameState;
	private int team0Score;
	private int team1Score;
	private int totalInnings;

	public FuzzballGame(int givenNumInnings) {
		this.totalInnings = givenNumInnings;
		this.gameState = TOP_OF_INNING_FLAG; // Ensures it's the top of the inning
		this.gameState |= 1 << INNING_SHIFT; // Sets the inning to 1
		resetPitchCount(); // Resets pitch count
		clearBases(); // Clears bases
		
	}
	public int getTotalInnings() {
        return this.totalInnings;
    }
	// Consolidate whichInning and getCurrentInning methods
	public int getCurrentInning() {
		return (gameState >> INNING_SHIFT) & INNING_MASK;
	}

	public boolean gameEnded() {
	    int currentInning = getCurrentInning();
	    boolean beyondLastInning = currentInning > totalInnings;
	    boolean lastInningBottomOver = (currentInning == totalInnings) && !isTopOfInning() && (team0Score != team1Score);
	    return beyondLastInning || lastInningBottomOver;
	}


	public boolean isTopOfInning() {
		return (gameState & TOP_OF_INNING_FLAG) != 0;
	}

	// Additional methods and corrections...

	private void resetPitchCount() {
		setBalls(0);
		setStrikes(0);
	}
	private void clearBases() {
		// Assuming the bases are represented in the gameState using BASES_MASK
		gameState &= ~BASES_MASK; // Clear the bases part in gameState
	}


	private void advanceRunnersOnHit(int basesToAdvance) {
		int currentBases = gameState & BASES_MASK;
		int runsScored = 0;

		// Shift the current bases to advance the runners.
		int newBases = currentBases << basesToAdvance;

		// Calculate runs scored based on how many runners "overflow" past the third base.
		runsScored += Integer.bitCount(newBases >> 3); // Count bits shifted out of the 3-base mask.

		// Ensure the batter's advancement onto base is included, and apply the BASES_MASK to wrap around correctly.
		newBases = (newBases | (1 << (basesToAdvance - 1))) & BASES_MASK;

		// Update gameState with new base configuration.
		gameState = (gameState & ~BASES_MASK) | newBases;

		// Update scores based on the inning.
		if (isTopOfInning()) {
			team0Score += runsScored;
		} else {
			team1Score += runsScored;
		}
		scoreRuns(runsScored); 
	}
	public void ball() {
	    int balls = getBalls() + 1;
	    if (balls >= MAX_BALLS) {
	        advanceRunnerOnWalk();
	        balls = 0; // Reset balls count after a walk
	    }
	    setBalls(balls);
	}

	private void advanceRunnerOnWalk() {
		int bases = gameState & BASES_MASK;
		boolean basesLoaded = (bases == 0x07); // Check if all bases are occupied (binary 111)

		// Advance runners only if forced
		if (basesLoaded || (bases & 0x01) != 0) { // If bases are loaded or first base is occupied
			bases = (bases << 1) | 0x01; // Shift runners and place new runner on first
			if (basesLoaded) {
				// If bases were loaded, increase score for the current team
				if (isTopOfInning()) {
					team0Score++;
				} else {
					team1Score++;
				}
			}
		} else {
			// Place a runner on first base if it was empty
			bases |= 0x01;
		}

		// Update the gameState with the new bases configuration
		gameState = (gameState & ~BASES_MASK) | (bases & BASES_MASK);

		// Reset balls for the next batter
		setBalls(0);
	}
	public void caughtFly() {
	    if (gameEnded()) return; // Early exit if the game has ended

	    int outs = getCurrentOuts() + 1;
	    if (outs >= MAX_OUTS) {
	        outs = 0; // Reset outs
	        switchSides(); // Correctly handle inning transition
	    }
	    setOuts(outs); // Update outs count
	}
	    private void switchSides() {
	        // Toggle the top/bottom inning flag
	        gameState ^= TOP_OF_INNING_FLAG;

	        // If transitioning from bottom to top, increment inning
	        if (!isTopOfInning()) { // Check after toggling
	            incrementInnings();
	        }

	        clearBases(); // Clear bases for new side
	        resetPitchCount(); // Reset balls and strikes
	    }

    // Increment the inning count
	private void incrementInnings() {
	    int currentInning = (gameState >> INNING_SHIFT) & INNING_MASK;
	    currentInning++; // Increment inning
	    gameState = (gameState & ~(INNING_MASK << INNING_SHIFT)) | (currentInning << INNING_SHIFT); // Set new inning
	}


	public int getBallCount() {

		int balls=(int) getBalls();

	
		return balls;
	}

	// Correcting getBalls() to align with expectations from SimpleTests.java
	// Ensure this method is correctly named as expected by the test cases.
	public int getBalls() {
		return (gameState >> BALLS_SHIFT) & 0x03; // Extract balls count using BALLS_SHIFT.
	}

	public String getBases() {
		// Construct a String representation for the bases.
		StringBuilder bases = new StringBuilder();
		for (int i = 0; i < 3; i++) {
			bases.append(((gameState & BASES_MASK) & (1 << i)) != 0 ? 'X' : 'o');
		}
		return bases.toString();
	}



	public int getCalledStrikes() {
		// Assuming strikes are stored in gameState using STRIKES_SHIFT
		return (gameState >> STRIKES_SHIFT) & 0x03; // Extract the number of strikes.
	}
	// This method gets the current number of outs.
	public int getCurrentOuts() {
		return (gameState >> OUTS_SHIFT) & 0x03;
	}

	public int getOuts() {
		return (gameState >> OUTS_SHIFT) & 0x03;
	}

	public int getStrikes() {
		return (gameState >> STRIKES_SHIFT) & 0x03;
	}

	public int getTeam0Score() {
		// Return team 0's score. Adjust if scoring logic involves bitwise operations.
		return team0Score;
	}

	public int getTeam1Score() {
		// Return team 1's score. Adjust if scoring logic involves bitwise operations.
		return team1Score;
	}

	public void hit(int distance) {
		if (distance < 15) {
			// Handle fouls: increment strikes unless it would result in the third strike.
			if (getStrikes() < MAX_STRIKES - 1) {
				setStrikes(getStrikes() + 1);
			} else {
				// If this foul would result in the third strike, treat the batter as out.
				caughtFly(); // This method should handle incrementing outs and switching sides if necessary.
			}
			return; // Exit the method as the play is concluded for this batter.
		}

		// Calculate the number of bases to advance for valid hits.
		int basesToAdvance = 0;
		if (distance < 150) {
			basesToAdvance = 1; // Single
		} else if (distance < 200) {
			basesToAdvance = 2; // Double
		} else if (distance < 250) {
			basesToAdvance = 3; // Triple
		} else {
			basesToAdvance = 4; // Home Run
		}

		// Advance runners based on the hit.
		advanceRunnersOnHit(basesToAdvance);
	}

	public boolean runnerOnBase(int which) {
		// Check if a runner is on the specified base (1 for first, 2 for second, 3 for third)
		int basePosition = 1 << (which - 1); // Calculate bit position for the base.
		return (gameState & BASES_MASK & basePosition) != 0;
	}


	private void setBalls(int balls) {
		gameState = (gameState & ~(0x03 << BALLS_SHIFT)) | (balls << BALLS_SHIFT);
	}

	private void setOuts(int outs) {
		gameState = (gameState & ~(0x03 << OUTS_SHIFT)) | (outs << OUTS_SHIFT);
	}

	private void setStrikes(int strikes) {
		gameState = (gameState & ~(0x03 << STRIKES_SHIFT)) | (strikes << STRIKES_SHIFT);
	}

	public void strike(boolean swung) {
		if (gameEnded()) return;
		int strikes = getStrikes();
		if (swung || strikes >= MAX_STRIKES - 1) {
			caughtFly();
			setStrikes(0);
		} else {
			setStrikes(strikes + 1);
		}
	}

	private void scoreRuns(int runs) {
		if (isTopOfInning()) {
			team0Score += runs;
		} else {
			team1Score += runs;
		}
	}

	public int whichInning() {

		return (gameState >> INNING_SHIFT) & INNING_MASK;
	}		
	public boolean isGameOver() {
		return whichInning() > getNumberOfInnings();
	}
	private int getNumberOfInnings() {
		// TODO Auto-generated method stub
		return  (gameState >> INNING_SHIFT) & INNING_MASK;
	}
}
