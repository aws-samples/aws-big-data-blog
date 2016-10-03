CREATE TABLE public.vendortranssummary (
	"vendor_id" varchar(25),
	"item_id" varchar(25),
	"trans_date" date,
	"sale_amount" numeric(5,2),
	"tax_amount" numeric(5,2),
	"discount_amount" numeric(5,2)
)
compound sortkey(vendor_id,item_id,trans_date);
