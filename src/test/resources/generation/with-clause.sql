WITH ex AS ( SELECT col2 from tab2 )
SELECT col, ex.col2 INTO x, y
FROM tab, ex;