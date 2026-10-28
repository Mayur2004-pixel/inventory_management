-- ============================================================================
-- SmartStock — Inventory Management System
-- Script: 02_create_triggers.sql
-- Description: Business integrity triggers for product audit and immutable history
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Trigger 1: Automatically update UPDATED_AT timestamp on PRODUCTS
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_PRODUCT_UPDATE_TIMESTAMP
BEFORE UPDATE ON PRODUCTS
FOR EACH ROW
BEGIN
    :NEW.UPDATED_AT := CURRENT_TIMESTAMP;
END;
/

-- ----------------------------------------------------------------------------
-- Trigger 2: Prevent physical deletion of products referenced in STOCK_MOVEMENTS
--            Enforces soft-delete / deactivation policy.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_PREVENT_PRODUCT_DELETE
BEFORE DELETE ON PRODUCTS
FOR EACH ROW
DECLARE
    v_movement_count NUMBER := 0;
BEGIN
    SELECT COUNT(*)
    INTO v_movement_count
    FROM STOCK_MOVEMENTS
    WHERE PRODUCT_ID = :OLD.PRODUCT_ID;

    IF v_movement_count > 0 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'BUSINESS RULE VIOLATION: Product "' || :OLD.PRODUCT_NAME || '" (ID: ' || :OLD.PRODUCT_ID ||
            ') has ' || v_movement_count || ' historical stock movements. Physical deletion is prohibited. Set STATUS to INACTIVE instead.'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- Trigger 3: Prevent mutation or deletion of audit logs in STOCK_MOVEMENTS
--            Guarantees permanent, tamper-proof inventory transaction history.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_PREVENT_HISTORY_MUTATION
BEFORE UPDATE OR DELETE ON STOCK_MOVEMENTS
FOR EACH ROW
BEGIN
    IF DELETING THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'AUDIT INTEGRITY VIOLATION: Stock movement records are immutable and cannot be deleted.'
        );
    ELSIF UPDATING THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'AUDIT INTEGRITY VIOLATION: Stock movement records are immutable and cannot be modified.'
        );
    END IF;
END;
/
