// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

interface IBlacklist {
    function isBlacklisted(address _account) external view returns (bool);
}

contract ISTCoin is ERC20 {
    IBlacklist public blacklistContract;

    constructor(address _blacklistContract) ERC20("IST Coin", "IST") {
        _mint(msg.sender, 100_000_000 * 10**2); // 100 million tokens with 2 decimals
        blacklistContract = IBlacklist(_blacklistContract);
    }

    function _beforeTokenTransfer(address from, address to, uint256 amount) internal override {
        require(!blacklistContract.isBlacklisted(from), "Sender is blacklisted");
        require(!blacklistContract.isBlacklisted(to), "Receiver is blacklisted");
        super._beforeTokenTransfer(from, to, amount);
    }
}
