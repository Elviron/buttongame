package com.ilves.buttongame;

import java.util.ArrayList;
import java.util.Random;

import android.util.Log;
import android.util.SparseBooleanArray;

public class GameThread extends Thread {
	
	private MainActivity		parent;
	
	public boolean				isPlaying		= false;
	private ArrayList<Integer>	list;
	public int					currentButton	= -1;
	private int					newButton		= -1;
	public int					score;
	private SparseBooleanArray	map;
	public int					gameTime;
	private long				endTime;

	public GameThread(MainActivity parent, ArrayList<Integer> buttonList) {
		this.parent = parent;
		list = buttonList;
		map = new SparseBooleanArray();
		for (int i = 0; i < list.size(); i++) {
			map.put(list.get(i), false);
		}
		score = 0;
		currentButton = list.get(new Random().nextInt(8));
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		int tester = -1;
		score = 0;
		parent.hit(score, -1, currentButton);
		endTime = System.currentTimeMillis() + gameTime;
		while (System.currentTimeMillis() < endTime) {
			// Do game loop
			for (int i = 0; i < list.size(); i++) {
				tester = list.get(i);
				if (map.get(tester)) {
					// The button has been pressed
					if (tester == currentButton) {
						// Pressed the correct button
						// increase score
						score += 10;
						// get a new button
						do {
							newButton = list.get(new Random().nextInt(8));
						} while (newButton == currentButton);
						// switch the button backgrounds
						parent.hit(score, currentButton, newButton);
						// save the new button
						currentButton = newButton;
					} else {
						// Pressed the wrong button
						// decrease score
						score -= 5;
						// dont update the buttons
						parent.hit(score, -1, currentButton);
					}
					// reset value now that we have checked it
					map.put(tester, false);
				}
			}
		}
		// Now game is over, display score screen
		Log.i(MainActivity.TAG, "Game ended.");
		parent.gameEnded(score);
		// super.run();
	}

	public void buttonPressed(int numberId) {
		map.put(numberId, true);
	}

}
