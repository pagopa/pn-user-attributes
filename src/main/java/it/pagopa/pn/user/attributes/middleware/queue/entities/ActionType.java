package it.pagopa.pn.user.attributes.middleware.queue.entities;

public enum ActionType {
    IO_ACTIVATED_ACTION() { //NEW
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_io_activated_%s",
                    action.getActionId(),
                    action.getInternalId()
            );
        }
    },
    
    SEND_MESSAGE_ACTION() { //NEW
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_send_message_%s",
                    action.getActionId(),
                    action.getInternalId()
            );
        }
    },

    PEC_REJECTED_ACTION() { //NEW
        @Override
        public String buildActionId(Action action) {
            return String.format(
                    "%s_pec_rejected_%s",
                    action.getActionId(),
                    action.getInternalId()
            );
        }
    };

    public String buildActionId(Action action) {
        throw new UnsupportedOperationException("Must be implemented for each action type");
    }
}
