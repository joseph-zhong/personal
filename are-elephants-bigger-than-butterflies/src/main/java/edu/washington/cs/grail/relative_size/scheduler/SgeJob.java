package edu.washington.cs.grail.relative_size.scheduler;

class SgeJob {
	private String script;
	private String[] args;
	private JobStatus status = JobStatus.NOT_SUBMITTED;
	private String jobId;
	private String message;
	
	public SgeJob(String script, String[] args) {
		this.script = script;
		this.args = args;
	}
	
	public JobStatus getStatus() {
		return status;
	}
	
	public String getJobId() {
		return jobId;
	}
	
	void setJobId(String jobId) {
		this.jobId = jobId;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	String getCommand() {
		StringBuilder sb = new StringBuilder(script);
		for (String arg : args)
			sb.append(arg);
		return sb.toString();
	}
	
	void setStatus(JobStatus status) {
		this.status = status;
	}
}