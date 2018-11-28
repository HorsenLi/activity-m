package org.trustnote.activity.service.impl;

import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustnote.activity.common.enume.LockStatesEnum;
import org.trustnote.activity.common.pojo.DepositLock;
import org.trustnote.activity.common.pojo.GenerateAddress;
import org.trustnote.activity.common.utils.OkHttpUtils;
import org.trustnote.activity.service.iface.DepositService;
import org.trustnote.activity.skeleton.mybatis.mapper.DepositLockMapper;
import org.trustnote.activity.skeleton.mybatis.mapper.GenerateAddressMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author zhuxl 18-2-6
 * @since v0.3
 */
@Service
public class DepositServiceImpl implements DepositService {
    private static final Logger logger = LogManager.getLogger(DepositServiceImpl.class);

    @Autowired
    private DepositLockMapper depositLockMapper;
    @Autowired
    private GenerateAddressMapper generateAddressMapper;

    @Override
    public String insert(final String address, final int status) {
        final DepositLock depositLock = DepositLock.builder()
                .packageType(status)
                .receiptAddress(address)
                .lockStatus(LockStatesEnum.PENDING_LOCK.getCode())
                .lockTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        final List<GenerateAddress> generateAddressList = this.generateAddressMapper.selectByExample(null);
        final GenerateAddress generateAddress = generateAddressList.get(generateAddressList.size() - 1);
        depositLock.setWalletAddress(generateAddress.getAddress());
        this.depositLockMapper.insert(depositLock);
        return generateAddress.getAddress();
    }

    @Override
    public boolean querybalance(final String address) {
        final BigDecimal dataBigDecimal = this.query(address);
        if (dataBigDecimal.compareTo(new BigDecimal(0)) != 0) {
            return true;
        }
        return false;
    }

    public void queryPayed(final List<DepositLock> depositLockList) {
        depositLockList.stream()
                .forEach(depositLock -> {
                    final BigDecimal dataBigDecimal = this.query(depositLock.getWalletAddress());
                    depositLock.setLockAmount(dataBigDecimal);
                    final Integer packageType = depositLock.getPackageType();
                    if (dataBigDecimal.compareTo(new BigDecimal(0)) != 0) {
                        BigDecimal rate = null;
                        BigDecimal numericalv = null;
                        if (packageType == 1) {
                            rate = new BigDecimal(0.3);
                            numericalv = new BigDecimal(180);
                        } else {
                            rate = new BigDecimal(0.4);
                            numericalv = new BigDecimal(360);
                        }
                        //计算收益
                        final BigDecimal all = dataBigDecimal.multiply(numericalv).multiply(rate);
                        final BigDecimal income = all.divide(new BigDecimal(360), 1, BigDecimal.ROUND_DOWN);
                        final BigDecimal tfans = income.multiply(new BigDecimal(10));
                        depositLock.setIncomeAmount(income);
                        depositLock.setTfsAmount(tfans);
                        depositLock.setLockStatus(LockStatesEnum.LOCKING.getCode());
                        depositLock.setReturnAmount(income.add(dataBigDecimal));
                        depositLock.setLockTime(LocalDateTime.now());
                        depositLock.setUpdateTime(LocalDateTime.now());
                    }
                    depositLock.setUnlockTime(depositLock.getLockTime().plusDays(packageType == 1 ? 180 : 360));
                    this.depositLockMapper.updateByPrimaryKeySelective(depositLock);
                });
    }

    public BigDecimal query(final String address) {
        final String url = "http://150.109.57.242:6002/api/v1/asset/balance/" + address + "/TTT";
        final String result = OkHttpUtils.get(url, null);
        final JSONObject parse = (JSONObject) JSONObject.parse(result);
        final JSONObject data = parse.getJSONObject("data");
        final BigDecimal dataBigDecimal = data.getBigDecimal("pending");
        return dataBigDecimal;
    }
}
