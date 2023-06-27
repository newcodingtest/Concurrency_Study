package com.example.stock.domain;


import javax.persistence.*;


@Entity
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    private Long productId;

    private Long quantity;

   // @Version//Optimistic Lock  일때만 필요
    //private Long version;

    public Stock(){
        
    }
    public Stock(Long productId, Long quantity){
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getQuantity(){
        return this.quantity;
    }

    public void decrease(Long quantity){
      if(this.quantity-quantity<0){
         throw new RuntimeException("quantity is 0");
      }
      this.quantity = this.quantity - quantity;
    }
}
