package me.vinceh121.gmcserver.managers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Promise;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.Record;

public class TimelineManager extends AbstractManager {

	public TimelineManager(final GMCServer srv) {
		super(srv);
	}

	public class InterpolatedTimelineAction extends AbstractAction<List<Record>> {
		private Iterable<Record> records;
		private int outputCount = 50;
		private TimeUnit scale = TimeUnit.DAYS;

		public InterpolatedTimelineAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<List<Record>> promise) {
			for (int i = 0; i < outputCount; i++) {

			}
		}

	}

}
