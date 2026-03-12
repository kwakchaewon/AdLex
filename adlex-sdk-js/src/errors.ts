/** AdLex SDK 예외 클래스 */

export class AdlexError extends Error {
  constructor(
    message: string,
    public readonly statusCode?: number
  ) {
    super(message);
    this.name = "AdlexError";
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

export class AuthenticationError extends AdlexError {
  constructor(message: string) {
    super(message, 401);
    this.name = "AuthenticationError";
  }
}

export class RateLimitError extends AdlexError {
  constructor(message: string) {
    super(message, 429);
    this.name = "RateLimitError";
  }
}

export class QuotaExceededError extends AdlexError {
  constructor(message: string) {
    super(message, 402);
    this.name = "QuotaExceededError";
  }
}

export class PlanUpgradeRequiredError extends AdlexError {
  constructor(message: string) {
    super(message, 402);
    this.name = "PlanUpgradeRequiredError";
  }
}

export class NotFoundError extends AdlexError {
  constructor(message: string) {
    super(message, 404);
    this.name = "NotFoundError";
  }
}

export class ValidationError extends AdlexError {
  constructor(message: string) {
    super(message, 400);
    this.name = "ValidationError";
  }
}

export class ServerError extends AdlexError {
  constructor(message: string, statusCode: number) {
    super(message, statusCode);
    this.name = "ServerError";
  }
}
