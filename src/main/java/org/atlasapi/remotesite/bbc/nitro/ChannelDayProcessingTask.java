package org.atlasapi.remotesite.bbc.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.atlasapi.media.channel.Channel;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;

/**
 * {@link ScheduledTask} which processes a range of {@link Channel}s and
 * {@link LocalDate} days via a {@link ChannelDayProcessor}.
 */
public final class ChannelDayProcessingTask extends ScheduledTask {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final ListeningExecutorService executor;
    private final Supplier<? extends Collection<ChannelDay>> channelDays;
    private final ChannelDayProcessor processor;
    private final ChannelDayProcessingTaskListener listener;

    private AtomicInteger processed;
    private int tasks;
    private AtomicReference<UpdateProgress> progress;

    public ChannelDayProcessingTask(ExecutorService executor, Supplier<? extends Collection<ChannelDay>> channelDays, ChannelDayProcessor processor) {
        this(executor, channelDays, processor, null);
    }
    
    public ChannelDayProcessingTask(ExecutorService executor, Supplier<? extends Collection<ChannelDay>> channelDays, ChannelDayProcessor processor,
            ChannelDayProcessingTaskListener listener) {
        this.listener = listener;
        this.executor = MoreExecutors.listeningDecorator(executor);
        this.channelDays = checkNotNull(channelDays);
        this.processor = checkNotNull(processor);
    }

    private void updateStatus() {
        reportStatus(String.format("%s/%s %s", processed, tasks, progress));
    }
    
    @Override
    protected void runTask() {
        
        Collection<ChannelDay> channels = channelDays.get();
        Iterator<ChannelDay> channelsIter = channels.iterator();

        processed = new AtomicInteger();
        tasks = channels.size();
        progress = new AtomicReference<UpdateProgress>(UpdateProgress.START);
        
        ImmutableList.Builder<ListenableFuture<UpdateProgress>> results
            = ImmutableList.builder();
        while(channelsIter.hasNext() && shouldContinue()) {
            results.add(submitTask(channelsIter.next()));
            updateStatus();
        }
        
        waitForFinish(results.build());
        
        if (listener != null) {
            listener.completed(progress.get());
        }
    }

    private void waitForFinish(ImmutableList<ListenableFuture<UpdateProgress>> taskResults) {
        final CountDownLatch cdl = new CountDownLatch(1);
        ListenableFuture<List<UpdateProgress>> results = Futures.successfulAsList(taskResults);
        Futures.addCallback(results, new FutureCallback<List<UpdateProgress>>() {
            @Override
            public void onSuccess(List<UpdateProgress> result) {
                cdl.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                cdl.countDown();
            }
        });
        try {
            while (shouldContinue() && !cdl.await(5, TimeUnit.SECONDS)) {}
        } catch (InterruptedException ie) {
            return;
        }
        if (!results.isDone() && !shouldContinue()) {
            results.cancel(true);
        }
    }

    private ListenableFuture<UpdateProgress> submitTask(final ChannelDay cd) {
        log.debug("submit: {}", cd);
        ListenableFuture<UpdateProgress> taskResult = executor.submit(new ChannelDayProcessTask(cd, listener));
        Futures.addCallback(taskResult, new ProgressUpdatingCallback());
        return taskResult;
    }

    private final class ProgressUpdatingCallback implements FutureCallback<UpdateProgress> {

        @Override
        public void onSuccess(UpdateProgress result) {
            UpdateProgress cur = progress.get();
            while(!progress.compareAndSet(cur, cur.reduce(result))) {
                cur = progress.get();
            }
            processed.incrementAndGet();
            updateStatus();
        }

        @Override
        public void onFailure(Throwable t) {
            log.error(t.getMessage(),t);
            updateStatus();
        }
    }

    private final class ChannelDayProcessTask implements Callable<UpdateProgress> {

        private final ChannelDay cd;
        private final ChannelDayProcessingTaskListener listener;

        private ChannelDayProcessTask(ChannelDay cd, ChannelDayProcessingTaskListener listener) {
            this.cd = cd;
            this.listener = listener;
        }

        @Override
        public UpdateProgress call() throws Exception {
            System.out.println(Thread.currentThread().getName());
            if (!shouldContinue()) {
                return UpdateProgress.START;
            }
            try {
                UpdateProgress progress = processor.process(cd);
                
                if (listener != null) {
                    listener.channelDayCompleted(cd, progress);
                }
                return progress;
            } catch (Exception e) {
                log.error(cd.toString(), e);
                return UpdateProgress.FAILURE;
            }
        }
    }

}
