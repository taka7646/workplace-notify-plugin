package com.taka7646.workplace;

import hudson.model.AbstractBuild;
import hudson.model.Result;

public enum NotificationStrategy {

	ALL("all"),
	FAILURE("failure"),
	SUCCESS("success"),
	;

	private String value;

	NotificationStrategy(String value) {
		this.value = value;
	}

	public static NotificationStrategy fromString(String text) {
		if (text != null) {
			for (NotificationStrategy b : NotificationStrategy.values()) {
				if (text.equalsIgnoreCase(b.value)) {
					return b;
				}
			}
		}
		return null;
	}
	
	public boolean needNotification(AbstractBuild<?, ?> build){
		Result result = build.getResult();
		if(result.completeBuild){
			//　成功のとき
			return this.value == ALL.value || this.value == SUCCESS.value;
		}else{
			// 失敗のとき
			return this.value == ALL.value || this.value == FAILURE.value;
		}
	}
}
