export interface OpenAIListResponse<T> {
  object: 'list';
  data: T[];
  first_id?: string;
  last_id?: string;
  has_more?: boolean;
}

export interface OpenAIErrorResponse {
  error: {
    message: string;
    type: string;
    param?: string;
    code?: string;
  };
}
