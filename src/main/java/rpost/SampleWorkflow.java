package rpost;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.LocalActivityOptions;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.workflow.*;

import java.time.Duration;

@WorkflowInterface
public interface SampleWorkflow {

    @WorkflowMethod
    public void execute(String mutexWorkflowId);

    public class SampleWorkflowImpl implements SampleWorkflow {
        @Override
        public void execute(String mutexWorkflowId) {
            WorkflowInfo currentWorkflowInfo = io.temporal.workflow.Workflow.getInfo();
            String currentTaskQueue = currentWorkflowInfo.getTaskQueue();
            SignalWithStartActivity activity = io.temporal.workflow.Workflow.newLocalActivityStub(
                    SignalWithStartActivity.class,
                    LocalActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(5))
                            .build()
            );

            activity.signalWithStart(mutexWorkflowId, currentTaskQueue);
        }
    }

    @ActivityInterface
    public interface SignalWithStartActivity {

        @ActivityMethod
        String signalWithStart(String mutexWorkflowId, String taskQueue);

    }

    public class SignalWithStartActivityImpl implements SignalWithStartActivity {

        private final WorkflowClient workflowClient;

        public SignalWithStartActivityImpl(WorkflowClient workflowClient) {
            this.workflowClient = workflowClient;
        }

        public String signalWithStart(String mutexWorkflowId, String taskQueue) {
            MutexWorkflow mutexWorkflow = workflowClient.newWorkflowStub(
                    MutexWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(mutexWorkflowId)
                            .setTaskQueue(taskQueue)
                            .build()
            );
            WorkflowStub workflowStub = WorkflowStub.fromTyped(mutexWorkflow);
            WorkflowExecution workflowExecution = workflowStub.signalWithStart("signal", new Object[]{}, new Object[]{});
            return workflowExecution.getWorkflowId();

        }
    }

    @WorkflowInterface
    public interface MutexWorkflow {

        @WorkflowMethod
        public void execute();

        @SignalMethod
        public void signal();

    }

    public class MutexWorkflowImpl implements MutexWorkflow {
        @Override
        public void execute() {
            Workflow.sleep(Duration.ofSeconds(3));
        }

        @Override
        public void signal() {

        }
    }

}

