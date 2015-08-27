create function f_return_focused_specialty(full_physician_specialty varchar)

 RETURNS varchar
 STABLE 
AS $$

	parts = full_physician_specialty.split("/")
	return parts[len(parts) - 1].strip()
 
$$LANGUAGE plpythonu
