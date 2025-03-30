// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract Blacklist is Ownable {
    constructor() Ownable(msg.sender) {}  // Pass deployer as initial owner

    mapping(address => bool) private blacklisted;
    address[] private blacklistedAddresses;

    event AddedToBlacklist(address indexed account);
    event RemovedFromBlacklist(address indexed account);

    function addToBlacklist(address account) external onlyOwner returns (bool) {
        require(!blacklisted[account], "Account already blacklisted");
        blacklisted[account] = true;
        blacklistedAddresses.push(account); 
        emit AddedToBlacklist(account);
        return true;
    }

    function removeFromBlacklist(address account) external onlyOwner returns (bool) {
        require(blacklisted[account], "Account is not blacklisted");
        blacklisted[account] = false;
        for (uint i = 0; i < blacklistedAddresses.length; i++) {
            if (blacklistedAddresses[i] == account) {
                blacklistedAddresses[i] = blacklistedAddresses[blacklistedAddresses.length - 1];
                blacklistedAddresses.pop();
                break;
            }
        }

        emit RemovedFromBlacklist(account);
        return true;
    }

    function isBlacklisted(address account) public view returns (bool) {
        return blacklisted[account];
    }

    function getBlacklist() public view returns (address[] memory) {
        return blacklistedAddresses;
    }
}


contract ISTCoin is ERC20, Blacklist {
    constructor() ERC20("IST Coin", "IST") {
        _mint(msg.sender, 100000000 * 10**2); // 100 million tokens with 2 decimals
    }

    function _update(address from, address to, uint256 value) internal override {
        require(!isBlacklisted(from), "Sender is blacklisted");
        require(!isBlacklisted(to), "Receiver is blacklisted");
        super._update(from, to, value); // Call the parent function
    }
}

