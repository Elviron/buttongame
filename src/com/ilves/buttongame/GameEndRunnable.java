package com.ilves.buttongame;

import android.view.View;
import android.widget.TextView;

public class GameEndRunnable implements
		Runnable {

	private MainActivity	parent;
	private int				score;
	private int				gameTime;

	public GameEndRunnable(MainActivity parent, int score, int gameTime) {
		this.parent = parent;
		this.score = score;
		this.gameTime = gameTime;
	}

	@Override
	public void run() {
		// set scores
		parent.debugLog(""+score);
		((TextView) parent.findViewById(R.id.score_points)).setText(""+this.score);
		float persec = score / gameTime;
		parent.debugLog(""+persec);
		((TextView) parent.findViewById(R.id.score_points_per_sec)).setText(String.valueOf(persec));
		// Show score overlay
		parent.findViewById(R.id.score_overlay).setVisibility(View.VISIBLE);
		parent.submitScores(score, gameTime);

	}
}
