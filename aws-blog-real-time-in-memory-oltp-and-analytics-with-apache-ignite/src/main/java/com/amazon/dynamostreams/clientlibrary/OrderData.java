package com.amazon.dynamostreams.clientlibrary;

public class OrderData {
	private String orderId;
	private long orderDate;
	private String shipMethod;
	private String billAddress;
	private String billCity;
	private int billPostalCode;
	private int orderQty;
	private int unitPrice;
	private String productCategory;

	private OrderData() {
	};

	public OrderData(String orderId, long orderDate, String shipMethod, String billAddress, String billCity,
			int billPostalCode, int orderQty, int unitPrice, String productCategory) {
		super();
		this.orderId = orderId;
		this.orderDate = orderDate;
		this.shipMethod = shipMethod;
		this.billAddress = billAddress;
		this.billCity = billCity;
		this.billPostalCode = billPostalCode;
		this.orderQty = orderQty;
		this.unitPrice = unitPrice;
		this.productCategory = productCategory;
	}

	public String getOrderId() {
		return orderId;
	}

	public long getOrderDate() {
		return orderDate;
	}

	public String getShipMethod() {
		return shipMethod;
	}

	public String getBillAddress() {
		return billAddress;
	}

	public String getBillCity() {
		return billCity;
	}

	public int getBillPostalCode() {
		return billPostalCode;
	}

	public int getUnitPrice() {
		return unitPrice;
	}

	public String getProductCategory() {
		return productCategory;
	}

}
