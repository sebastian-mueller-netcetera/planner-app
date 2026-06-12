export type TaskStatus = 'BACKLOG' | 'IN_PROGRESS' | 'DONE';

export interface TaskAssignee {
  id: string;
  email: string;
  displayName: string;
}

export interface TaskLabel {
  id: string;
  name: string;
  color: string;
}

export interface TaskSprint {
  id: string;
  name: string;
}

export interface TaskListItem {
  id: string;
  title: string;
  status: TaskStatus;
  dueDate: string | null;
  assignee: TaskAssignee | null;
  labels: TaskLabel[];
  sprint: TaskSprint | null;
  createdAt: string;
  updatedAt: string;
}

export interface TaskPage {
  content: TaskListItem[];
  totalElements: number;
  totalPages: number;
  page: number;
}

export interface TaskRequest {
  title: string;
  description?: string;
  status?: TaskStatus;
  dueDate?: string | null;
  assigneeId?: string | null;
  labelIds?: string[];
  sprintId?: string | null;
}

export interface Comment {
  id: string;
  body: string;
  author: TaskAssignee;
  createdAt: string;
  updatedAt: string;
}

export interface Task extends TaskListItem {
  description: string | null;
  comments: Comment[];
}

export interface TaskQueryParams {
  q?: string;
  status?: TaskStatus | '';
  assigneeId?: string;
  labelId?: string;
  sprintId?: string;
  page?: number;
  size?: number;
}

export interface SprintSummary {
  id: string;
  name: string;
  isoYear: number;
  isoWeek: number;
  startDate: string;
  endDate: string;
  status: string;
}
