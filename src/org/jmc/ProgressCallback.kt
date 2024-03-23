package org.jmc;

/**
 * Simple callback to indicate progress of an operation.
 */
interface ProgressCallback
{
	/**
	 * Sets the current level of progress.
	 * @param value Progress, in the interval [0,1]
	 */
	fun setProgress(value: Float);

	/**
	 * Sets a message to describe currently running task.
	 * @param message The current message, null for no message.
	 */
	fun setMessage(message: String);
}
