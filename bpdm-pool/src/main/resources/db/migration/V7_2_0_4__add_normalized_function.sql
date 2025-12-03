CREATE OR REPLACE FUNCTION bpdm.normalize_name(input text)
RETURNS text AS
$$
BEGIN
    IF input IS NULL THEN
        RETURN NULL;
    END IF;

    RETURN regexp_replace(
               lower(
                   replace(
                       replace(
                           replace(
                               replace(trim(input), 'ä', 'ae'),
                               'ö', 'oe'
                           ),
                           'ü', 'ue'
                       ),
                       'ß', 'ss'
                   )
               ),
               '\s+', ' ', 'g'
           );
END;
$$ LANGUAGE plpgsql IMMUTABLE;
