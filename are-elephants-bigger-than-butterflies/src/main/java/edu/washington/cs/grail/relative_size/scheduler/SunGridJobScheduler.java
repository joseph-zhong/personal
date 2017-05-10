package edu.washington.cs.grail.relative_size.scheduler;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import edu.washington.cs.grail.relative_size.utils.ShellExecuter;

public class SunGridJobScheduler {
	private JobSubmitter[] submitters;
	private Queue<SgeJob> jobQueue = new LinkedList<SgeJob>();

	public SunGridJobScheduler(int numConcurrentJobs) {
		submitters = new JobSubmitter[numConcurrentJobs];
	}

	public void submitJob(String script, String... args) {
		synchronized (jobQueue) {
			jobQueue.add(new SgeJob(script, args));
			jobQueue.notify();
		}
	}
	
	public void start(){
		for (JobSubmitter submitter: submitters)
			submitter.start();
	}
	
	public void die(){
		for (JobSubmitter submitter: submitters)
			submitter.die();
	}

	private class JobSubmitter extends Thread {
		private static final int WAIT_TIME_MILLIS = 10 * 1000;
		boolean alive = true;

		@Override
		public void run() {
			while (alive) {
				SgeJob job = null;
				// Get the job from queue:
				synchronized (jobQueue) {
					while (jobQueue.isEmpty()) {
						try {
							jobQueue.wait(WAIT_TIME_MILLIS);
							if (!alive)
								return;
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						job = jobQueue.poll();
					}
				}

				// Run the job:
				try {
					String jobId = new ShellExecuter(
							"qsub "
									+ job.getCommand()
									+ " | awk 'match($0,/[0-9]+/){print substr($0, RSTART, RLENGTH)}'")
							.execute();
					job.setJobId(jobId);
					job.setStatus(JobStatus.WAITING_QUEUE);
				} catch (IOException e) {
					job.setStatus(JobStatus.SUBMIT_FAILED);
					job.setMessage(e.getMessage());
				} catch (InterruptedException e) {
					job.setStatus(JobStatus.SUBMIT_FAILED);
					job.setMessage(e.getMessage());
				}
				
				
				// Check the job status:
				
				
				

			}
		}

		public void die() {
			alive = false;
		}
	}
}