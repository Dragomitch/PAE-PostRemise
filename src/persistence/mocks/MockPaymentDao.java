package persistence.mocks;

import business.dto.PaymentDto;
import persistence.PaymentDao;

import java.util.ArrayList;
import java.util.List;

public class MockPaymentDao implements PaymentDao {

  private List<PaymentDto> payments;

  public MockPaymentDao() {
    payments = new ArrayList<PaymentDto>();
  }

  @Override
  public List<PaymentDto> findAll() {
    return payments;
  }

  public void addPayment(PaymentDto payment) {
    payments.add(payment);
  }

  public void empty() {
    payments = new ArrayList<PaymentDto>();
  }

}
