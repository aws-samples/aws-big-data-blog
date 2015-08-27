create function f_return_general_specialty(full_physician_specialty varchar)

 RETURNS varchar
 STABLE 
AS $$

	parts = full_physician_specialty.split("/")
	return parts[0].strip()
 
$$LANGUAGE plpythonu
