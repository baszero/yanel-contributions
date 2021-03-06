#!/bin/bash

PSQL="psql -U postgres konakart5"

sales_out="sales-volumes"
orders_out="orders-overview"
products_out="products-overview"

##
## Setup date variables
##

year1=$(date -d 'last month' +%Y)
month1=$(date -d 'last month' +%m)

year2=$(date +%Y)
month2=$(date +%m)
today=$(date +%F)

timestamp1="$year1-$month1-01 00:00"
timestamp2="$year2-$month2-01 00:00"
timestamp3=$(date -d 'last month' +'%F %R')

##
## Sales values
##

$PSQL <<-EOF > ${sales_out}-${year1}-${month1}.csv
	copy (
	select
		'$year1-$month1' as month,
		sum(t.value) as sales_volume
	from
		orders o, orders_total t
	where
		o.orders_id = t.orders_id and
		t.class = 'ot_total' and
		o.date_purchased >= (timestamp '$timestamp1') and
		o.date_purchased < (timestamp '$timestamp2'))
	to stdout header csv;
EOF

##
## Order overview
##

$PSQL <<-EOF > ${orders_out}-${today}.csv
	copy (
	select
		o.orders_id, t.value,
		o.date_purchased, s.orders_status_name, o.delivery_postcode
	from
		orders o, orders_total t, orders_status s
	where
		o.orders_id = t.orders_id and
		t.class = 'ot_total' and
		s.orders_status_id = o.orders_status and
		s.language_id = 2 and
		o.date_purchased >= (timestamp '$timestamp3'))
	to stdout header csv;
EOF

##
## Products overview
##

$PSQL <<-EOF > ${products_out}-${year1}-${month1}.csv
	copy (
	select
		p.products_id, d.products_name, p.products_model, sum(p.products_quantity), 
		sum(p.products_quantity*p.final_price) as sales_volume
	from
		orders o, orders_products p, products_description d
	where
		p.orders_id = o.orders_id and
		d.products_id = p.products_id and
		o.date_purchased >= (timestamp '$timestamp1') and
		o.date_purchased < (timestamp '$timestamp2')
	group by
		p.products_id, d.products_name, p.products_model
	order by sales_volume desc)
	to stdout header csv;
EOF
