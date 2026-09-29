import { isProblemDetail, isProblemFieldError } from './problem-detail';

describe('isProblemDetail', () => {
  it('accepts a complete EMA problem', () => {
    expect(
      isProblemDetail({
        type: 'urn:pae:problem:username-taken',
        title: 'Conflit',
        status: 409,
        detail: 'Ce nom est pris.',
        instance: '/api/1.0/users',
        code: 'USERNAME_TAKEN',
        errors: [{ field: 'username', code: 'Unique', message: 'Pris' }],
      }),
    ).toBeTrue();
  });

  it('accepts a minimal problem carrying only one identifying member', () => {
    expect(isProblemDetail({ code: 'X' })).toBeTrue();
    expect(isProblemDetail({ title: 'Oops' })).toBeTrue();
    expect(isProblemDetail({ detail: 'Oops' })).toBeTrue();
    expect(isProblemDetail({ type: 'about:blank', status: 500 })).toBeTrue();
  });

  it('accepts null optional members', () => {
    expect(isProblemDetail({ code: 'X', detail: null, status: null, errors: null })).toBeTrue();
  });

  it('rejects non-objects', () => {
    for (const value of [null, undefined, 'text', 42, true, [], ['code']]) {
      expect(isProblemDetail(value)).withContext(String(value)).toBeFalse();
    }
  });

  it('accepts prototype-less objects but rejects class instances such as ProgressEvent', () => {
    expect(isProblemDetail(Object.assign(Object.create(null), { code: 'X' }))).toBeTrue();
    expect(isProblemDetail(new ProgressEvent('error'))).toBeFalse();
    expect(isProblemDetail(new Date())).toBeFalse();
  });

  it('rejects objects without identifying members', () => {
    expect(isProblemDetail({})).toBeFalse();
    expect(isProblemDetail({ status: 500 })).toBeFalse();
    expect(isProblemDetail({ code: '', title: '' })).toBeFalse();
    expect(isProblemDetail({ timestamp: 'x', error: 'Internal Server Error' })).toBeFalse();
  });

  it('rejects members with the wrong type', () => {
    expect(isProblemDetail({ code: 1 })).toBeFalse();
    expect(isProblemDetail({ code: 'X', status: '500' })).toBeFalse();
    expect(isProblemDetail({ code: 'X', detail: {} })).toBeFalse();
    expect(isProblemDetail({ code: 'X', errors: {} })).toBeFalse();
    expect(isProblemDetail({ code: 'X', instance: 3 })).toBeFalse();
  });
});

describe('isProblemFieldError', () => {
  it('requires field and message strings', () => {
    expect(isProblemFieldError({ field: 'email', message: 'Invalide' })).toBeTrue();
    expect(isProblemFieldError({ field: 'email', code: 'Email', message: 'Invalide' })).toBeTrue();
    expect(isProblemFieldError({ field: 'email' })).toBeFalse();
    expect(isProblemFieldError({ message: 'Invalide' })).toBeFalse();
    expect(isProblemFieldError({ field: 'email', message: 'x', code: 3 })).toBeFalse();
    expect(isProblemFieldError('email')).toBeFalse();
  });
});
