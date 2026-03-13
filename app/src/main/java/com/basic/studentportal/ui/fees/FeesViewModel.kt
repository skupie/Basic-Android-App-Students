package com.basic.studentportal.ui.fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basic.studentportal.data.model.FeePayment
import com.basic.studentportal.data.model.InvoicesResponse
import com.basic.studentportal.data.model.ListResponse
import com.basic.studentportal.data.repository.FeeRepository
import com.basic.studentportal.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeesViewModel @Inject constructor(private val repository: FeeRepository) : ViewModel() {

    private val _invoices = MutableStateFlow<Resource<InvoicesResponse>>(Resource.Loading)
    val invoices: StateFlow<Resource<InvoicesResponse>> = _invoices

    private val _payments = MutableStateFlow<Resource<ListResponse<FeePayment>>>(Resource.Loading)
    val payments: StateFlow<Resource<ListResponse<FeePayment>>> = _payments

    init {
        loadInvoices()
        loadPayments()
    }

    fun loadInvoices() {
        viewModelScope.launch {
            _invoices.value = Resource.Loading
            _invoices.value = repository.getInvoices()
        }
    }

    fun loadPayments() {
        viewModelScope.launch {
            _payments.value = Resource.Loading
            _payments.value = repository.getPayments()
        }
    }

    fun refresh() { loadInvoices(); loadPayments() }
}
