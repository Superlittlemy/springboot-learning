package com.slm.validator.service;

import com.slm.validator.Application;
import com.slm.validator.request.Address;
import com.slm.validator.request.UserModel;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;

@Service
@Validated
public class UserService {

    public void createUser(UserModel userModel) {

    }

    public void modifyUser(UserModel userModel) {
        Address address = new Address();
        address.setValue(userModel.getAddress());
        Application.sa.getBean(this.getClass()).addressPersistence(address);
    }

    public void addressPersistence(@Valid Address address) {

    }

}
