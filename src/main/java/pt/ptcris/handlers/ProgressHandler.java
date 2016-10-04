package pt.ptcris.handlers;

/**
 * An interface for reporting the progress of the synchronization procedures and
 * the comprising tasks.
 */
public interface ProgressHandler {

	/**
	 * Used to set the progress of a task, by using percentual values (between 0
	 * and 100). It can be used to start a task, where progress = 0
	 *
	 * @param progress
	 *            the progress of the current task
	 */
	public void setProgress(int progress);

	/**
	 * Used to set the text of the current task
	 *
	 * @param message
	 *            the message to be sent to the progress handler
	 */
	public void setCurrentStatus(String message);

	/**
	 * Used to send an error message to the progress handler
	 *
	 * @param message
	 *            the error message to be sent to the progress handler
	 */
	public void sendError(String message);

	/**
	 * Used to set the current task as finalized
	 */
	public void done();
}
