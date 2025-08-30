/**
 * Contains the worker components responsible for executing asynchronous jobs in the system.
 *
 * <p>Classes in this package include:</p>
 * <ul>
 *   <li>{@link com.acme.api.asynctaskqueue.worker.JobHandler} - Interface defining the contract for job handlers.</li>
 *   <li>{@link com.acme.api.asynctaskqueue.worker.EmailJobHandler} - Implementation of {@code JobHandler} for email jobs.</li>
 *   <li>{@link com.acme.api.asynctaskqueue.worker.ReportJobHandler} - Implementation of {@code JobHandler} for report generation jobs.</li>
 *   <li>{@link com.acme.api.asynctaskqueue.worker.JobHandlerBootstrap} - Initializes and configures job handlers at application startup.</li>
 *   <li>{@link com.acme.api.asynctaskqueue.worker.JobHandlerRegistry} - Maintains a registry of available job handlers.</li>
 * </ul>
 *
 * <p>This package is primarily concerned with executing jobs, managing their handlers, and supporting
 * job registration and bootstrapping within the asynchronous task queue.</p>
 */
package com.acme.api.asynctaskqueue.worker;
