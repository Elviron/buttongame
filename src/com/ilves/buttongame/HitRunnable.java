package com.ilves.buttongame;

import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class HitRunnable implements
		Runnable {

	private MainActivity	parent;
	private int				score;
	private Integer			oldButton;
	private Integer			newButton;

	public HitRunnable(MainActivity parent, int score, Integer oldButton,
			Integer newButton) {
		this.parent = parent;
		this.score = score;
		Log.i(MainActivity.TAG, "Score: " + score);
		this.oldButton = oldButton;
		this.newButton = newButton;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		((TextView) parent.findViewById(R.id.text_score)).setText("Score: " + score);
		if (oldButton >= 0) {
			((Button) parent.findViewById(oldButton)).setBackgroundResource(R.drawable.gametheme_btn_default_holo_light);
		}
		((Button) parent.findViewById(newButton)).setBackgroundResource(R.drawable.gametheme_btn_default_holo_light_green);
	}

}
