ALTER TABLE task_errors DROP CONSTRAINT task_errors_type_check;
ALTER TABLE task_errors ADD CONSTRAINT task_errors_type_check CHECK (type IN (
    'Timeout',
    'Unspecified',
    'NaturalPersonError',
    'BpnErrorNotFound',
    'BpnErrorTooManyOptions',
    'MandatoryFieldValidationFailed',
    'BlacklistCountryPresent',
    'UnknownSpecialCharacters'
));