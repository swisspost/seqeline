SELECT c INTO n FROM (
    SELECT col c FROM tab
    UNION
    SELECT col2 c FROM tab2
);
