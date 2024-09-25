ALTER TABLE golden_record_tasks
DROP CONSTRAINT golden_record_tasks_task_step_state_check;

ALTER TABLE golden_record_tasks
ADD CONSTRAINT golden_record_tasks_task_step_state_check
check ( task_step_state in ('Queued', 'Reserved', 'Success', 'Error', 'Aborted'));

ALTER TABLE golden_record_tasks
DROP CONSTRAINT golden_record_tasks_task_result_state_check;

ALTER TABLE golden_record_tasks
ADD CONSTRAINT golden_record_tasks_task_result_state_check
check ( task_result_state in ('Pending', 'Success', 'Error', 'Aborted'));
